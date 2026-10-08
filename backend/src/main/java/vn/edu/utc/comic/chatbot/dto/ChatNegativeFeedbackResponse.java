package vn.edu.utc.comic.chatbot.dto;

import java.time.Instant;

/** Một câu trả lời bị chấm "không hữu ích", để quản trị viên xem lại và chỉnh prompt. */
public record ChatNegativeFeedbackResponse(Long messageId, String conversationTitle, String reply, Instant createdAt) {
}
