package vn.edu.utc.comic.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import vn.edu.utc.comic.chatbot.dto.ChatHistoryEntry;
import vn.edu.utc.comic.chatbot.dto.ChatTurn;
import vn.edu.utc.comic.chatbot.dto.ChatbotAnswer;
import vn.edu.utc.comic.chatbot.dto.StoredRecommendation;
import vn.edu.utc.comic.chatbot.dto.ToolTraceEntry;
import vn.edu.utc.comic.chatbot.enums.ChatRole;
import vn.edu.utc.comic.chatbot.tool.ChatToolDescriptions;
import vn.edu.utc.comic.genre.dto.GenrePromptItem;
import vn.edu.utc.comic.genre.service.GenreService;

/**
 * Dựng danh sách tin nhắn gửi cho mô hình: system prompt (kèm danh sách thể loại và định dạng JSON bắt buộc),
 * lịch sử hội thoại có tóm tắt, và câu hỏi hiện tại.
 *
 * <p>Truyền thẳng các đối tượng Message thay vì dùng {@code .system(String)} của ChatClient: văn bản ở đó có thể
 * bị coi là template, mà JSON schema của định dạng trả lời chứa đầy dấu ngoặc nhọn.
 */
@Slf4j
@Component
public class ChatPromptBuilder {

    private static final String GENRES_PLACEHOLDER = "{genres}";
    private static final String GENRE_LINE = "- %s — %s: %s";
    private static final String LINE_SEPARATOR = "\n";

    private final String systemTemplate;
    private final GenreService genreService;
    private final ObjectMapper objectMapper;
    private final BeanOutputConverter<ChatbotAnswer> outputConverter = new BeanOutputConverter<>(ChatbotAnswer.class);

    public ChatPromptBuilder(@Value("classpath:prompts/chatbot-system.txt") Resource systemPrompt,
                             GenreService genreService, ObjectMapper objectMapper) {
        this.systemTemplate = readResource(systemPrompt);
        this.genreService = genreService;
        this.objectMapper = objectMapper;
    }

    /** Toàn bộ tin nhắn cho một lượt hỏi, theo đúng thứ tự gửi đi. */
    public List<Message> buildMessages(ChatTurn turn) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemText()));
        messages.addAll(buildHistory(turn.history()));
        messages.add(new UserMessage(turn.content()));
        return messages;
    }

    /** Đọc câu trả lời có cấu trúc từ văn bản mô hình trả về (bỏ lời dẫn hoặc rào ```json nếu có). */
    public ChatbotAnswer parseAnswer(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Câu trả lời không chứa JSON");
        }
        return outputConverter.convert(text.substring(start, end + 1));
    }

    private String buildSystemText() {
        String genres = genreService.findPromptGenres().stream()
                .map(ChatPromptBuilder::formatGenre)
                .collect(Collectors.joining(LINE_SEPARATOR));
        return systemTemplate.replace(GENRES_PLACEHOLDER, genres) + LINE_SEPARATOR + outputConverter.getFormat();
    }

    private static String formatGenre(GenrePromptItem genre) {
        return GENRE_LINE.formatted(genre.slug(), genre.name(), genre.description() == null ? "" : genre.description());
    }

    /**
     * Lịch sử theo thứ tự thời gian. Anthropic yêu cầu tin đầu tiên (sau system) là của người dùng và hai bên nói
     * xen kẽ, nên bỏ các tin của trợ lý đứng đầu cửa sổ và chỉ giữ tin cuối trong chuỗi tin cùng một bên.
     */
    private List<Message> buildHistory(List<ChatHistoryEntry> history) {
        List<Message> messages = new ArrayList<>();
        ChatRole lastRole = ChatRole.ASSISTANT;
        for (ChatHistoryEntry entry : history) {
            if (entry.role() == lastRole) {
                if (!messages.isEmpty()) {
                    messages.removeLast();
                } else {
                    continue;
                }
            }
            messages.add(entry.role() == ChatRole.USER
                    ? new UserMessage(entry.content())
                    : new AssistantMessage(entry.content() + buildSummary(entry)));
            lastRole = entry.role();
        }
        // Câu hỏi hiện tại là tin của người dùng: tin cuối của lịch sử phải là của trợ lý
        if (lastRole == ChatRole.USER && !messages.isEmpty()) {
            messages.removeLast();
        }
        return messages;
    }

    /** "[Hàm đã gọi: ... Đã gợi ý: #12 "X" (320 chương)]" để mô hình hiểu "ngắn hơn", "cái thứ hai". */
    private String buildSummary(ChatHistoryEntry entry) {
        List<ToolTraceEntry> calls = readList(entry.toolTrace(), new TypeReference<>() { });
        List<StoredRecommendation> recommendations = readList(entry.recommendations(), new TypeReference<>() { });
        if (calls.isEmpty() && recommendations.isEmpty()) {
            return "";
        }
        String callText = calls.isEmpty() ? ChatToolDescriptions.HISTORY_NONE : calls.stream()
                .map(call -> call.tool() + " " + toJson(call.arguments()))
                .collect(Collectors.joining(ChatToolDescriptions.HISTORY_SEPARATOR));
        String storyText = recommendations.isEmpty() ? ChatToolDescriptions.HISTORY_NONE : recommendations.stream()
                .map(story -> MessageFormat.format(ChatToolDescriptions.HISTORY_STORY,
                        String.valueOf(story.storyId()), story.title(), String.valueOf(story.chapterCount())))
                .collect(Collectors.joining(ChatToolDescriptions.HISTORY_SEPARATOR));
        return MessageFormat.format(ChatToolDescriptions.HISTORY_SUMMARY, callText, storyText);
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            log.warn("Bỏ qua dữ liệu lịch sử chatbot không đọc được", exception);
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private static String readResource(Resource resource) {
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Không đọc được system prompt của chatbot", exception);
        }
    }
}
