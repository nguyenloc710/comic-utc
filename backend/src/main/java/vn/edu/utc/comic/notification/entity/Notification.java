package vn.edu.utc.comic.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.CreatedAtEntity;
import vn.edu.utc.comic.notification.enums.NotificationType;

/**
 * Một thông báo gửi tới một người dùng.
 *
 * <p>Người nhận lưu dạng id thay vì quan hệ tới UserAccount: thông báo chương mới được ghi hàng loạt bằng
 * một câu INSERT ... SELECT và chỉ đọc ra dạng DTO, không bao giờ cần nạp thực thể người nhận.
 */
@Getter
@Setter
@Entity
@Table(name = "notification")
public class Notification extends CreatedAtEntity {

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    /** Mảng JSON các tham số điền vào câu thông báo (tên truyện, số chương, lý do...). */
    @Column(name = "message_args", columnDefinition = "JSON")
    private String messageArgs;

    /** Đường dẫn tương đối mở ra khi bấm vào thông báo. */
    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;
}
