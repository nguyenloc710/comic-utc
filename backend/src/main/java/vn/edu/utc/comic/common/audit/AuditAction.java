package vn.edu.utc.comic.common.audit;

/** Hành động được ghi vào nhật ký kiểm toán. Thêm giá trị mới khi có thao tác nhạy cảm tương ứng. */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGED,
    USER_BANNED,
    USER_UNBANNED,
    AUTHOR_REQUEST_APPROVED,
    AUTHOR_REQUEST_REJECTED,
    STORY_DELETED
}
