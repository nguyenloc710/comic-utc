package vn.edu.utc.comic.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Một truyện mô hình gợi ý: id chép đúng từ kết quả hàm và một câu lý do. */
public record Recommendation(
        @JsonPropertyDescription("id của truyện, chép đúng từ kết quả hàm")
        Long storyId,
        @JsonPropertyDescription("Một câu tiếng Việt nói vì sao truyện hợp với yêu cầu")
        String reason) {
}
