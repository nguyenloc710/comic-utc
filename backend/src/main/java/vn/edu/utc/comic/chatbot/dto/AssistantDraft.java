package vn.edu.utc.comic.chatbot.dto;

import java.util.List;
import vn.edu.utc.comic.chatbot.enums.AnswerSource;
import vn.edu.utc.comic.chatbot.enums.FallbackReason;

/**
 * Câu trả lời của trợ lý đã sẵn sàng để lưu.
 *
 * @param cards         thẻ truyện đã qua hậu kiểm, đúng thứ tự hiển thị
 * @param rejectedCount số gợi ý của mô hình bị hậu kiểm loại
 */
public record AssistantDraft(
        AnswerSource source,
        FallbackReason fallbackReason,
        String reply,
        List<ChatCardResponse> cards,
        List<ToolTraceEntry> toolTrace,
        int rejectedCount,
        Integer promptTokens,
        Integer completionTokens) {
}
