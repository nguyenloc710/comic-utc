package vn.edu.utc.comic.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.notification.entity.Notification;

/** Truy vấn thông báo. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
