package vn.edu.utc.comic.chatbot.dto;

import java.time.Instant;
import java.util.List;
import vn.edu.utc.comic.chatbot.enums.AnswerSource;
import vn.edu.utc.comic.chatbot.enums.ChatRole;

/**
 * Một tin nhắn khi nạp lại hội thoại. Thẻ truyện được dựng lại từ cơ sở dữ liệu lúc nạp, nên truyện đã bị ẩn
 * từ sau không còn hiện ra.
 *
 * @param feedback 1 / -1 nếu người dùng đã đánh giá câu trả lời; {@code null} nếu chưa
 */
public record ChatMessageResponse(
        Long id,
        ChatRole role,
        String content,
        List<ChatCardResponse> cards,
        AnswerSource source,
        Integer feedback,
        Instant createdAt) {
}
