package vn.edu.utc.comic.chatbot.dto;

import java.time.Instant;

/** Một hội thoại trong danh sách của khung chat. */
public record ChatConversationResponse(Long id, String title, Instant lastMessageAt) {
}
