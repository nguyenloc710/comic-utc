package vn.edu.utc.comic.system.dto;

import vn.edu.utc.comic.common.audit.AuditAction;

/**
 * Bộ lọc nhật ký kiểm toán.
 *
 * @param actor một phần tên đăng nhập của người thực hiện
 */
public record AuditLogFilterRequest(AuditAction action, String actor) {
}
