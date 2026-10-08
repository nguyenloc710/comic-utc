package vn.edu.utc.comic.chatbot.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.chatbot.entity.ChatConversation;

/** Truy vấn hội thoại chatbot. Mọi truy vấn theo người dùng đều lọc theo id chủ hội thoại. */
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    /** Tìm kèm chủ: id hội thoại của người khác được coi như không tồn tại. */
    Optional<ChatConversation> findByIdAndUserId(Long id, Long userId);

    List<ChatConversation> findByUserIdOrderByLastMessageAtDescIdDesc(Long userId, Pageable pageable);

    /** Id các hội thoại không có tin nhắn mới từ một mốc, cũ nhất trước (để dọn theo lô). */
    @Query("SELECT c.id FROM ChatConversation c WHERE c.lastMessageAt < :cutoff ORDER BY c.lastMessageAt ASC")
    List<Long> findIdsInactiveSince(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChatConversation c WHERE c.id IN :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);
}
