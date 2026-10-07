package vn.edu.utc.comic.system.dto;

import java.time.Instant;
import vn.edu.utc.comic.common.audit.AuditAction;

/**
 * Một dòng nhật ký kiểm toán trên trang quản trị.
 *
 * @param detail JSON thông tin thêm của thao tác; có thể {@code null}
 */
public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorName,
        AuditAction action,
        String entityType,
        String entityId,
        String detail,
        String ipAddress,
        Instant createdAt) {
}
