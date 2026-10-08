package vn.edu.utc.comic.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.edu.utc.comic.common.constant.ChatbotConstants;

/**
 * Một tin nhắn người dùng gửi cho chatbot. Độ dài tối đa thật sự đọc từ setting; giới hạn ở đây chỉ chặn request
 * khổng lồ trước khi tới service.
 */
@Schema(description = "Tin nhắn gửi chatbot")
public record ChatMessageRequest(
        @Schema(description = "Hội thoại đang tiếp tục; bỏ trống để bắt đầu hội thoại mới") Long conversationId,
        @NotBlank(message = "{validation.chat.content.required}")
        @Size(max = ChatbotConstants.MESSAGE_HARD_MAX_LENGTH, message = "{validation.chat.content.size}")
        String content) {
}
