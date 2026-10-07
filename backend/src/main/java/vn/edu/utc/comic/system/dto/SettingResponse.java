package vn.edu.utc.comic.system.dto;

import java.time.Instant;

/**
 * Một tham số vận hành trên trang quản trị.
 *
 * @param valueType INTEGER / DECIMAL / BOOLEAN / JSON / STRING — quyết định cách kiểm tra giá trị mới
 * @param readOnly  chỉ xem, không sửa được qua giao diện
 */
public record SettingResponse(
        String settingKey,
        String settingValue,
        String valueType,
        String groupName,
        String description,
        boolean readOnly,
        Instant updatedAt) {
}
