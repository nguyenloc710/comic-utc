package vn.edu.utc.comic.chatbot.dto;

/**
 * Một gợi ý đã qua hậu kiểm, lưu dạng JSON trong tin nhắn của trợ lý. Tên và số chương chép lại tại thời điểm
 * gợi ý để lượt sau tóm tắt cho mô hình ("đã gợi ý #12 'X' (320 chương)") mà không phải truy vấn lại.
 */
public record StoredRecommendation(Long storyId, String title, int chapterCount, String reason) {
}
