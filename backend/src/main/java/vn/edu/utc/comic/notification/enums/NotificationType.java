package vn.edu.utc.comic.notification.enums;

/**
 * Loại thông báo trong ứng dụng. Câu hiển thị dựng từ khóa {@code notification.<TYPE>} trong
 * messages.properties cùng tham số lưu ở cột message_args, nên sửa lời văn không phải sửa dữ liệu.
 */
public enum NotificationType {
    NEW_CHAPTER,
    AUTHOR_REQUEST_APPROVED,
    AUTHOR_REQUEST_REJECTED,
    CONTENT_HIDDEN,
    COMMENT_REPLIED
}
