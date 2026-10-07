package vn.edu.utc.comic.notification.mapper;

import org.mapstruct.Mapper;
import vn.edu.utc.comic.notification.dto.NotificationResponse;
import vn.edu.utc.comic.notification.entity.Notification;

/** Ánh xạ thông báo. */
@Mapper
public interface NotificationMapper {

    /**
     * @param message câu thông báo do service dựng từ loại và tham số (mapper không đọc messages.properties)
     */
    NotificationResponse toResponse(Notification notification, String message);
}
