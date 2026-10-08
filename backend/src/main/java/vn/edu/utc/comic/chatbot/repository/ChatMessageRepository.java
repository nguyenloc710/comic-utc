package vn.edu.utc.comic.chatbot.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.chatbot.dto.ChatReasonCount;
import vn.edu.utc.comic.chatbot.dto.ChatStatsSummary;
import vn.edu.utc.comic.chatbot.entity.ChatMessage;
import vn.edu.utc.comic.chatbot.enums.ChatRole;

/** Truy vấn tin nhắn chatbot. */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Các tin gần nhất của một hội thoại, MỚI NHẤT trước (service đảo lại khi dựng lịch sử). */
    List<ChatMessage> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    /** Số tin một người đã gửi từ một mốc, để áp hạn mức theo ngày. */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.conversation.userId = :userId AND m.role = :role AND m.createdAt >= :from
            """)
    long countByUserSince(@Param("userId") Long userId, @Param("role") ChatRole role, @Param("from") Instant from);

    /** Tìm kèm chủ hội thoại: tin nhắn của người khác được coi như không tồn tại. */
    @Query("""
            SELECT m FROM ChatMessage m JOIN FETCH m.conversation c
            WHERE m.id = :messageId AND c.userId = :userId
            """)
    Optional<ChatMessage> findOwned(@Param("messageId") Long messageId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.conversation.id IN :conversationIds")
    int deleteByConversationIdIn(@Param("conversationIds") List<Long> conversationIds);

    /** Tổng hợp các con số cho trang quản trị chatbot trong một truy vấn. */
    @Query("""
            SELECT new vn.edu.utc.comic.chatbot.dto.ChatStatsSummary(
                SUM(CASE WHEN m.role = vn.edu.utc.comic.chatbot.enums.ChatRole.USER THEN 1 ELSE 0 END),
                COUNT(DISTINCT m.conversation.id),
                COUNT(DISTINCT m.conversation.userId),
                SUM(CASE WHEN m.answerSource = vn.edu.utc.comic.chatbot.enums.AnswerSource.LLM THEN 1 ELSE 0 END),
                SUM(CASE WHEN m.answerSource = vn.edu.utc.comic.chatbot.enums.AnswerSource.FALLBACK_KEYWORD THEN 1 ELSE 0 END),
                AVG(CASE WHEN m.answerSource = vn.edu.utc.comic.chatbot.enums.AnswerSource.LLM THEN m.latencyMs END),
                AVG(m.promptTokens),
                AVG(m.completionTokens),
                SUM(m.rejectedCount),
                SUM(CASE WHEN m.feedback = 1 THEN 1 ELSE 0 END),
                SUM(CASE WHEN m.feedback = -1 THEN 1 ELSE 0 END))
            FROM ChatMessage m WHERE m.createdAt >= :from
            """)
    ChatStatsSummary summarizeSince(@Param("from") Instant from);

    @Query("""
            SELECT new vn.edu.utc.comic.chatbot.dto.ChatReasonCount(m.fallbackReason, COUNT(m))
            FROM ChatMessage m
            WHERE m.createdAt >= :from AND m.fallbackReason IS NOT NULL
            GROUP BY m.fallbackReason
            ORDER BY COUNT(m) DESC
            """)
    List<ChatReasonCount> countFallbackReasonsSince(@Param("from") Instant from);

    /** Câu trả lời bị chấm "không hữu ích" gần nhất, kèm hội thoại để quản trị viên xem lại câu hỏi. */
    @Query("""
            SELECT m FROM ChatMessage m JOIN FETCH m.conversation
            WHERE m.feedback = -1
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findRecentNegative(Pageable pageable);
}
