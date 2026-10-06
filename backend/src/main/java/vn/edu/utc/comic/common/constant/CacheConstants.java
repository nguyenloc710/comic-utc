package vn.edu.utc.comic.common.constant;

/**
 * Tên các vùng cache. Khai báo tập trung để tránh gõ sai chuỗi ở @Cacheable.
 */
public final class CacheConstants {

    public static final String SETTINGS = "settings";

    /** Vai trò, trạng thái, tên hiển thị hiện tại của tài khoản; đọc ở mọi request đã đăng nhập. */
    public static final String ACCOUNT_STATES = "accountStates";

    private CacheConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
