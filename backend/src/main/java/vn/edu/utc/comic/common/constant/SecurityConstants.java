package vn.edu.utc.comic.common.constant;

/**
 * Hằng số phục vụ xác thực và phân quyền.
 */
public final class SecurityConstants {

    /** Tiền tố vai trò theo quy ước của Spring Security. */
    public static final String ROLE_PREFIX = "ROLE_";

    /** Phải trùng tên các giá trị của enum Role. */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_AUTHOR = "AUTHOR";
    public static final String ROLE_ADMIN = "ADMIN";

    /** Biểu thức cho @PreAuthorize ở controller (lớp kiểm tra thứ hai sau SecurityConfig). */
    public static final String HAS_ROLE_USER = "hasRole('" + ROLE_USER + "')";
    public static final String HAS_ROLE_AUTHOR = "hasRole('" + ROLE_AUTHOR + "')";
    public static final String HAS_ROLE_ADMIN = "hasRole('" + ROLE_ADMIN + "')";

    /** Độ mạnh thuật toán BCrypt. */
    public static final int BCRYPT_STRENGTH = 12;

    /** Dùng khi bảng setting chưa có khóa tương ứng. */
    public static final int DEFAULT_PASSWORD_MIN_LENGTH = 8;
    public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
    public static final int DEFAULT_LOCK_MINUTES = 15;

    /** Tên tham số mang mã lỗi trên trang đăng nhập (giá trị là LoginError.code). */
    public static final String LOGIN_ERROR_PARAM = "error";

    /**
     * Mọi script, style, font đều phục vụ từ chính ứng dụng (WebJars) nên CSP chỉ cần 'self'.
     * style-src cho phép inline vì trình soạn thảo Quill gắn style trực tiếp lên phần tử.
     */
    public static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self'; font-src 'self' data:";

    private SecurityConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
