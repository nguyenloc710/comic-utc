package vn.edu.utc.comic.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.utc.comic.chatbot.enums.AnswerSource;
import vn.edu.utc.comic.chatbot.enums.ChatRole;
import vn.edu.utc.comic.chatbot.enums.FallbackReason;
import vn.edu.utc.comic.common.entity.CreatedAtEntity;

/**
 * Một tin nhắn trong hội thoại. Tin của trợ lý mang thêm dữ liệu chẩn đoán (nguồn trả lời, số token, độ trễ,
 * vết gọi hàm) phục vụ hỏi nối tiếp và thống kê ở trang quản trị.
 */
@Getter
@Setter
@Entity
@Table(name = "chat_message")
public class ChatMessage extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private ChatConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20, updatable = false)
    private ChatRole role;

    /** Văn bản thuần; giao diện hiển thị bằng textContent. */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** JSON các gợi ý đã qua hậu kiểm: [{storyId, title, chapterCount, reason}]. */
    @Column(name = "recommendations", columnDefinition = "JSON")
    private String recommendations;

    /** JSON vết gọi hàm của lượt này: [{tool, arguments, storyIds}]. */
    @Column(name = "tool_trace", columnDefinition = "JSON")
    private String toolTrace;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_source", length = 20)
    private AnswerSource answerSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_reason", length = 50)
    private FallbackReason fallbackReason;

    @Column(name = "rejected_count")
    private Integer rejectedCount;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    /** 1 = hữu ích, -1 = không hữu ích, {@code null} = chưa đánh giá. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "feedback")
    private Integer feedback;
}
