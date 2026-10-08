package vn.edu.utc.comic.chatbot.dto;

import java.util.List;
import vn.edu.utc.comic.chatbot.enums.AnswerSource;

/**
 * Câu trả lời của chatbot cho một tin nhắn.
 *
 * @param messageId id tin nhắn của trợ lý, để gửi đánh giá
 * @param reply     văn bản thuần, hiển thị bằng textContent
 * @param source    LLM, hoặc FALLBACK_KEYWORD khi mô hình lỗi và hệ thống tìm theo từ khóa thay
 */
public record ChatReplyResponse(
        Long conversationId,
        Long messageId,
        String reply,
        List<ChatCardResponse> cards,
        AnswerSource source) {
}
