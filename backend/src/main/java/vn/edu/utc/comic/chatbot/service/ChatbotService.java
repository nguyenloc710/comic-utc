package vn.edu.utc.comic.chatbot.service;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import vn.edu.utc.comic.chatbot.dto.AssistantDraft;
import vn.edu.utc.comic.chatbot.dto.ChatMessageRequest;
import vn.edu.utc.comic.chatbot.dto.ChatReplyResponse;
import vn.edu.utc.comic.chatbot.dto.ChatTurn;
import vn.edu.utc.comic.chatbot.dto.ChatbotAnswer;
import vn.edu.utc.comic.chatbot.dto.Recommendation;
import vn.edu.utc.comic.chatbot.dto.ValidatedRecommendations;
import vn.edu.utc.comic.chatbot.enums.AnswerSource;
import vn.edu.utc.comic.chatbot.enums.FallbackReason;
import vn.edu.utc.comic.chatbot.tool.StoryCatalogTools;
import vn.edu.utc.comic.chatbot.tool.ToolResultCollector;
import vn.edu.utc.comic.common.exception.ApiException;

/**
 * Trả lời một tin nhắn của người dùng (docs/04 §3).
 *
 * <p>Luồng: nhận câu hỏi và lưu (transaction 1) → gọi mô hình kèm các hàm tra cứu (NGOÀI transaction) → hậu
 * kiểm gợi ý → lưu câu trả lời (transaction 2). Mô hình lỗi, quá thời gian hay trả lời hỏng thì rơi về
 * {@link KeywordFallbackResponder}: người dùng luôn nhận được một câu trả lời.
 *
 * <p>Đây là một trong hai nơi duy nhất phụ thuộc Spring AI (cùng {@link StoryCatalogTools}); đổi nhà cung cấp chỉ
 * là đổi starter và cấu hình.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatClient.Builder chatClientBuilder;
    private final StoryCatalogTools storyCatalogTools;
    private final ChatHistoryService chatHistoryService;
    private final ChatPromptBuilder chatPromptBuilder;
    private final RecommendationValidator recommendationValidator;
    private final KeywordFallbackResponder fallbackResponder;
    private final Clock clock;

    /**
     * @throws ApiException CHAT_DISABLED, CHAT_MESSAGE_TOO_LONG, CHAT_DAILY_LIMIT, CHAT_CONVERSATION_NOT_FOUND —
     *                      các lỗi này xảy ra TRƯỚC khi gọi mô hình và không có gì được lưu
     */
    public ChatReplyResponse reply(Long userId, ChatMessageRequest request) {
        ChatTurn turn = chatHistoryService.startTurn(userId, request);
        long startedAt = clock.millis();
        AssistantDraft draft;
        try {
            draft = askModel(turn);
        } catch (RuntimeException exception) {
            FallbackReason reason = classify(exception);
            // Không ghi nội dung tin nhắn vào log: đó là dữ liệu của người dùng
            log.warn("Chatbot rơi về tìm theo từ khóa ({}) ở hội thoại {}", reason, turn.conversationId(), exception);
            draft = fallbackResponder.respond(turn.content(), reason);
        }
        int latencyMs = (int) (clock.millis() - startedAt);
        return chatHistoryService.finishTurn(turn, draft, latencyMs);
    }

    private AssistantDraft askModel(ChatTurn turn) {
        ToolResultCollector collector = new ToolResultCollector();
        ChatResponse response = chatClientBuilder.build().prompt()
                .messages(chatPromptBuilder.buildMessages(turn))
                .tools(storyCatalogTools)
                .toolContext(Map.of(ToolResultCollector.CONTEXT_KEY, collector))
                .call()
                .chatResponse();
        ChatbotAnswer answer = readAnswer(response, collector);
        Set<Long> allowed = new LinkedHashSet<>(turn.previousStoryIds());
        allowed.addAll(collector.storyIds());
        ValidatedRecommendations validated = recommendationValidator.validate(answer.recommendations(), allowed);
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        return new AssistantDraft(AnswerSource.LLM, null, answer.reply(), validated.cards(), collector.entries(),
                validated.rejectedCount(), usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens());
    }

    /**
     * Đọc câu trả lời có cấu trúc. Mô hình đôi khi trả văn bản thường thay vì JSON: khi đó vẫn dùng được lời đáp,
     * kèm các truyện của lần gọi hàm gần nhất (không có lý do riêng). Văn bản rỗng, hoặc trông như JSON mà
     * không đọc được, thì coi là câu trả lời hỏng.
     */
    private ChatbotAnswer readAnswer(ChatResponse response, ToolResultCollector collector) {
        String text = response == null || response.getResult() == null
                ? null
                : response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new InvalidModelResponseException("Mô hình trả lời rỗng", null);
        }
        try {
            ChatbotAnswer answer = chatPromptBuilder.parseAnswer(text);
            if (answer.reply() == null || answer.reply().isBlank()) {
                throw new InvalidModelResponseException("Câu trả lời thiếu lời đáp", null);
            }
            return answer;
        } catch (InvalidModelResponseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (text.trim().startsWith("{")) {
                throw new InvalidModelResponseException("Không đọc được JSON của mô hình", exception);
            }
            List<Recommendation> lastResults = collector.lastStoryIds().stream()
                    .map(storyId -> new Recommendation(storyId, null))
                    .toList();
            return new ChatbotAnswer(text.trim(), lastResults);
        }
    }

    /** Phân loại nguyên nhân để trang quản trị biết đường lui đang bù cho vấn đề gì. */
    private static FallbackReason classify(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidModelResponseException) {
                return FallbackReason.INVALID_RESPONSE;
            }
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException
                    || cause instanceof TimeoutException) {
                return FallbackReason.TIMEOUT;
            }
        }
        return FallbackReason.PROVIDER_ERROR;
    }

    /** Mô hình trả lời nhưng không dùng được (rỗng, JSON hỏng, thiếu lời đáp). */
    static class InvalidModelResponseException extends RuntimeException {

        InvalidModelResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
