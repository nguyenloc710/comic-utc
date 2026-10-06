package vn.edu.utc.comic.common.audit;

/** Hành động được ghi vào nhật ký kiểm toán. Thêm giá trị mới khi có thao tác quản trị tương ứng. */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT
}
