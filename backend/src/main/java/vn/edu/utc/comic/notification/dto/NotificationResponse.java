package vn.edu.utc.comic.notification.dto;

import java.time.Instant;
import vn.edu.utc.comic.notification.enums.NotificationType;

/**
 * Một thông báo để hiển thị.
 *
 * @param message câu thông báo đã dựng từ loại + tham số theo messages.properties
 * @param link    đường dẫn tương đối mở ra khi bấm vào; có thể {@code null}
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        String link,
        boolean read,
        Instant createdAt) {
}
