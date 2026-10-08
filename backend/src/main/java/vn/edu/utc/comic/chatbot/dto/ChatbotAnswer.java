package vn.edu.utc.comic.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Câu trả lời có cấu trúc mà mô hình phải trả về (Spring AI sinh JSON schema từ record này và gửi kèm prompt).
 * Mô tả trường là văn bản cho mô hình đọc, không hiển thị cho người dùng.
 */
public record ChatbotAnswer(
        @JsonPropertyDescription("Lời đáp tiếng Việt cho người dùng, văn bản thuần, ngắn gọn")
        String reply,
        @JsonPropertyDescription("Các truyện gợi ý; chỉ lấy truyện có trong kết quả hàm, có thể rỗng")
        List<Recommendation> recommendations) {

    public ChatbotAnswer {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
