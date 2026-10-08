package vn.edu.utc.comic.chatbot.dto;

import java.util.List;
import java.util.Set;

/**
 * Một lượt hỏi đã được nhận (tin của người dùng đã lưu), mang đủ thứ cần để gọi mô hình mà không phải giữ
 * transaction mở trong lúc chờ nhà cung cấp trả lời.
 *
 * @param history           các tin trước tin này, cũ nhất trước
 * @param previousStoryIds  id truyện các hàm đã trả ở những lượt trước trong cửa sổ lịch sử: gợi ý lặp lại một
 *                          truyện đã thấy ở lượt trước vẫn hợp lệ
 */
public record ChatTurn(
        Long userId,
        Long conversationId,
        String content,
        List<ChatHistoryEntry> history,
        Set<Long> previousStoryIds) {
}
