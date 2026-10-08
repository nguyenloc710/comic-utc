package vn.edu.utc.comic.chatbot.dto;

import vn.edu.utc.comic.chatbot.enums.ChatRole;

/**
 * Một tin nhắn cũ dùng để dựng lịch sử gửi cho mô hình (tách khỏi thực thể để dùng được ngoài transaction).
 *
 * @param recommendations JSON gợi ý đã lưu (tin của trợ lý); có thể {@code null}
 * @param toolTrace       JSON vết gọi hàm đã lưu (tin của trợ lý); có thể {@code null}
 */
public record ChatHistoryEntry(ChatRole role, String content, String recommendations, String toolTrace) {
}
