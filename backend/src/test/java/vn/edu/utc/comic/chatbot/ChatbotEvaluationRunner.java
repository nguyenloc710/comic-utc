package vn.edu.utc.comic.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.utc.comic.chatbot.dto.ChatTurn;
import vn.edu.utc.comic.chatbot.dto.ChatbotAnswer;
import vn.edu.utc.comic.chatbot.dto.ToolTraceEntry;
import vn.edu.utc.comic.chatbot.dto.ValidatedRecommendations;
import vn.edu.utc.comic.chatbot.service.ChatPromptBuilder;
import vn.edu.utc.comic.chatbot.service.RecommendationValidator;
import vn.edu.utc.comic.chatbot.tool.ChatToolDescriptions;
import vn.edu.utc.comic.chatbot.tool.StoryCatalogTools;
import vn.edu.utc.comic.chatbot.tool.ToolResultCollector;

/**
 * Chạy bộ câu hỏi đánh giá (resources/chatbot/eval-queries.tsv) với mô hình THẬT trên cơ sở dữ liệu dev có dữ liệu
 * demo, rồi ghi bảng số liệu ra target/chatbot-eval.md cho Chương 4 của báo cáo (docs/04 §9).
 *
 * <p>Tốn tiền và cần mạng nên không bao giờ chạy cùng bộ test thường. Chạy tay khi MySQL dev đang chạy:
 * {@code AI_API_KEY=... ./mvnw test -Dtest=ChatbotEvaluationRunner -Dchatbot.eval=true}. Không ghi gì vào cơ sở
 * dữ liệu: gọi thẳng mô hình với các hàm tra cứu, không đi qua ChatbotService.
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "AI_API_KEY", matches = ".+")
@EnabledIfSystemProperty(named = "chatbot.eval", matches = "true")
class ChatbotEvaluationRunner {

    private static final String QUERIES = "chatbot/eval-queries.tsv";
    private static final Path REPORT = Path.of("target", "chatbot-eval.md");
    private static final String NONE = "-";

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private StoryCatalogTools storyCatalogTools;

    @Autowired
    private ChatPromptBuilder chatPromptBuilder;

    @Autowired
    private RecommendationValidator recommendationValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void runEvaluation() throws IOException {
        List<EvalCase> cases = readCases();
        List<EvalResult> results = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            results.add(evaluate(evalCase));
        }
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, buildReport(results), StandardCharsets.UTF_8);
        assertThat(results).hasSameSizeAs(cases);
    }

    private EvalResult evaluate(EvalCase evalCase) {
        ToolResultCollector collector = new ToolResultCollector();
        ChatTurn turn = new ChatTurn(0L, 0L, evalCase.query(), List.of(), Set.of());
        long startedAt = System.currentTimeMillis();
        try {
            ChatResponse response = chatClientBuilder.build().prompt()
                    .messages(chatPromptBuilder.buildMessages(turn))
                    .tools(storyCatalogTools)
                    .toolContext(Map.of(ToolResultCollector.CONTEXT_KEY, collector))
                    .call()
                    .chatResponse();
            long latency = System.currentTimeMillis() - startedAt;
            ChatbotAnswer answer = chatPromptBuilder.parseAnswer(response.getResult().getOutput().getText());
            ValidatedRecommendations validated = recommendationValidator.validate(answer.recommendations(),
                    collector.storyIds());
            Usage usage = response.getMetadata().getUsage();
            return new EvalResult(evalCase, collector.entries(), validated.cards().size(), validated.rejectedCount(),
                    latency, usage.getPromptTokens(), usage.getCompletionTokens(), null);
        } catch (RuntimeException exception) {
            return new EvalResult(evalCase, collector.entries(), 0, 0, System.currentTimeMillis() - startedAt,
                    null, null, exception.getClass().getSimpleName());
        }
    }

    /** Chấm một câu: đúng loại hàm, và tham số chứa đủ các giá trị kỳ vọng. */
    private boolean isCorrect(EvalResult result) {
        EvalCase expected = result.evalCase();
        return switch (expected.kind()) {
            case "OUT_OF_SCOPE" -> result.calls().isEmpty() && result.cards() == 0 && result.error() == null;
            case "TRENDING" -> hasCall(result, ChatToolDescriptions.GET_TRENDING_STORIES_NAME, expected);
            case "SEARCH" -> hasCall(result, ChatToolDescriptions.SEARCH_STORIES_NAME, expected);
            default -> result.error() == null;
        };
    }

    private boolean hasCall(EvalResult result, String tool, EvalCase expected) {
        return result.calls().stream()
                .filter(call -> call.tool().equals(tool))
                .map(call -> toJson(call.arguments()))
                .anyMatch(arguments -> contains(arguments, expected.type())
                        && contains(arguments, expected.status())
                        && expected.genres().stream().allMatch(arguments::contains));
    }

    private static boolean contains(String arguments, String expectedValue) {
        return NONE.equals(expectedValue) || arguments.contains("\"" + expectedValue + "\"");
    }

    private String buildReport(List<EvalResult> results) {
        long correct = results.stream().filter(this::isCorrect).count();
        long errors = results.stream().filter(result -> result.error() != null).count();
        int rejected = results.stream().mapToInt(EvalResult::rejected).sum();
        double latency = results.stream().mapToLong(EvalResult::latencyMs).average().orElse(0);
        double promptTokens = results.stream().filter(result -> result.promptTokens() != null)
                .mapToInt(EvalResult::promptTokens).average().orElse(0);
        double completionTokens = results.stream().filter(result -> result.completionTokens() != null)
                .mapToInt(EvalResult::completionTokens).average().orElse(0);
        StringBuilder report = new StringBuilder("# Đánh giá chatbot\n\n")
                .append("| Chỉ số | Giá trị |\n|---|---|\n")
                .append("| Số câu | ").append(results.size()).append(" |\n")
                .append("| Đúng kỳ vọng (hàm + tham số / từ chối đúng) | ").append(correct).append(" (")
                .append(Math.round(correct * 100.0 / results.size())).append("%) |\n")
                .append("| Gợi ý bị hậu kiểm loại | ").append(rejected).append(" |\n")
                .append("| Lỗi gọi mô hình / đọc JSON | ").append(errors).append(" |\n")
                .append("| Độ trễ trung bình | ").append(Math.round(latency)).append(" ms |\n")
                .append("| Token trung bình (vào / ra) | ").append(Math.round(promptTokens)).append(" / ")
                .append(Math.round(completionTokens)).append(" |\n\n")
                .append("| Câu hỏi | Kỳ vọng | Đúng | Hàm đã gọi | Thẻ | Bị loại | ms | Lỗi |\n")
                .append("|---|---|---|---|---|---|---|---|\n");
        for (EvalResult result : results) {
            report.append("| ").append(result.evalCase().query())
                    .append(" | ").append(result.evalCase().kind())
                    .append(" | ").append(isCorrect(result) ? "✔" : "✘")
                    .append(" | ").append(result.calls().stream()
                            .map(call -> call.tool() + " `" + toJson(call.arguments()) + "`").toList())
                    .append(" | ").append(result.cards())
                    .append(" | ").append(result.rejected())
                    .append(" | ").append(result.latencyMs())
                    .append(" | ").append(result.error() == null ? "" : result.error())
                    .append(" |\n");
        }
        return report.toString();
    }

    private List<EvalCase> readCases() throws IOException {
        List<String> lines = new ClassPathResource(QUERIES).getContentAsString(StandardCharsets.UTF_8).lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .skip(1)
                .toList();
        return lines.stream().map(line -> line.split("\t")).map(columns -> new EvalCase(columns[0], columns[1],
                columns[2], columns[3], NONE.equals(columns[4])
                        ? Set.of()
                        : new LinkedHashSet<>(Arrays.asList(columns[4].split(","))))).toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            return String.valueOf(value);
        }
    }

    private record EvalCase(String query, String kind, String type, String status, Set<String> genres) {
    }

    private record EvalResult(EvalCase evalCase, List<ToolTraceEntry> calls, int cards, int rejected, long latencyMs,
                              Integer promptTokens, Integer completionTokens, String error) {
    }
}
