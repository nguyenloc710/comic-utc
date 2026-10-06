package vn.edu.utc.comic.common.constant;

/**
 * Tên các vùng cache. Khai báo tập trung để tránh gõ sai chuỗi ở @Cacheable.
 */
public final class CacheConstants {

    public static final String SETTINGS = "settings";

    /** Vai trò, trạng thái, tên hiển thị hiện tại của tài khoản; đọc ở mọi request đã đăng nhập. */
    public static final String ACCOUNT_STATES = "accountStates";

    /** Danh sách thể loại đang bật, dùng ở bộ lọc truyện và (sau này) prompt của chatbot. */
    public static final String GENRES = "genres";

    /** Bảng xếp hạng theo từng tiêu chí. */
    public static final String RANKINGS = "rankings";

    private CacheConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
