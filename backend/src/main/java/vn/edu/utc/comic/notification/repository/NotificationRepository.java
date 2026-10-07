package vn.edu.utc.comic.notification.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.notification.entity.Notification;

/** Truy vấn thông báo. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Thông báo của một người, mới nhất trước (id tự tăng nên id lớn hơn là mới hơn). */
    Page<Notification> findByRecipientIdOrderByIdDesc(Long recipientId, Pageable pageable);

    /** Tìm kèm người nhận để không ai mở được thông báo của người khác bằng cách đoán id. */
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllRead(@Param("recipientId") Long recipientId);

    /**
     * Ghi cùng một thông báo cho MỌI người đang theo dõi truyện bằng một câu lệnh, thay vì nạp danh sách người
     * theo dõi rồi lưu từng dòng — truyện có vài nghìn người theo dõi vẫn chỉ tốn một lượt đi về cơ sở dữ liệu.
     * Phải là SQL gốc vì JPQL không chèn được vào bảng từ một câu SELECT trên bảng khác với cột hằng.
     *
     * <p>Cố ý KHÔNG nối với bảng story ở đây: câu lệnh chỉ đọc story_follow, nên không giữ khóa trên dòng truyện
     * trong lúc một người khác đang bấm theo dõi (xem ghi chú về thứ tự khóa ở StoryRepository).
     *
     * @param messageArgs mảng JSON các tham số của câu thông báo
     * @return số thông báo đã ghi
     */
    @Modifying
    @Query(value = """
            INSERT INTO notification (recipient_id, type, message_args, link, is_read, created_at)
            SELECT f.user_id, :type, :messageArgs, :link, 0, :createdAt
            FROM story_follow f WHERE f.story_id = :storyId
            """, nativeQuery = true)
    int insertForFollowers(@Param("storyId") Long storyId, @Param("type") String type,
                           @Param("messageArgs") String messageArgs, @Param("link") String link,
                           @Param("createdAt") Instant createdAt);
}
