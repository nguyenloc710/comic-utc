package vn.edu.utc.comic.common.constant;

/**
 * Giới hạn cố định của dữ liệu tài khoản (khớp độ dài cột trong lược đồ).
 */
public final class UserConstants {

    /**
     * Tên đăng nhập chỉ gồm chữ không dấu, số, gạch dưới và dấu chấm. Cấm ký tự '@' để tên đăng nhập của
     * người này không bao giờ trùng email của người khác — ô đăng nhập nhận cả hai nên trùng là không
     * xác định được tài khoản.
     */
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_.]{3,30}$";

    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int DISPLAY_NAME_MAX_LENGTH = 100;
    public static final int BIO_MAX_LENGTH = 500;

    /** BCrypt chỉ dùng 72 byte đầu của mật khẩu; chặn trên để không ai gửi chuỗi khổng lồ bắt máy chủ băm. */
    public static final int PASSWORD_MAX_LENGTH = 72;

    private UserConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
