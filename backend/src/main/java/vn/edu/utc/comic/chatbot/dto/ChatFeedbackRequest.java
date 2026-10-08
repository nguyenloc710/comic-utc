package vn.edu.utc.comic.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Đánh giá một câu trả lời của chatbot. */
@Schema(description = "Đánh giá câu trả lời: 1 = hữu ích, -1 = không hữu ích, 0 = bỏ đánh giá")
public record ChatFeedbackRequest(
        @NotNull(message = "{validation.chat.feedback.range}")
        @Min(value = -1, message = "{validation.chat.feedback.range}")
        @Max(value = 1, message = "{validation.chat.feedback.range}")
        Integer value) {
}
