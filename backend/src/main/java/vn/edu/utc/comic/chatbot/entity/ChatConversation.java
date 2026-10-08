package vn.edu.utc.comic.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.CreatedAtEntity;

/**
 * Một cuộc trò chuyện của một người với chatbot.
 *
 * <p>Chủ hội thoại lưu dạng id: mọi truy vấn đều lọc theo id người đang đăng nhập, không bao giờ cần nạp thực thể
 * tài khoản.
 */
@Getter
@Setter
@Entity
@Table(name = "chat_conversation")
public class ChatConversation extends CreatedAtEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** Cắt từ tin nhắn đầu tiên, để người dùng nhận ra hội thoại trong danh sách. */
    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;
}
