package vn.edu.utc.comic.common.constant;

/**
 * Giới hạn cố định của yêu cầu đăng ký tác giả (khớp độ dài cột trong lược đồ).
 */
public final class AuthorConstants {

    public static final int PEN_NAME_MIN_LENGTH = 2;
    public static final int PEN_NAME_MAX_LENGTH = 100;

    /** Đủ dài để quản trị viên có căn cứ xét duyệt, không nhận một lời giới thiệu vài chữ. */
    public static final int INTRODUCTION_MIN_LENGTH = 20;
    public static final int INTRODUCTION_MAX_LENGTH = 2000;

    public static final int REJECT_REASON_MAX_LENGTH = 1000;

    /** Dùng khi bảng setting chưa có khóa tương ứng. */
    public static final int DEFAULT_REQUEST_COOLDOWN_DAYS = 7;

    private AuthorConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
