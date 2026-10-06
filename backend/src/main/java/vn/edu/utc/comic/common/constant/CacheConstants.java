package vn.edu.utc.comic.common.constant;

/**
 * Tên các vùng cache. Khai báo tập trung để tránh gõ sai chuỗi ở @Cacheable.
 */
public final class CacheConstants {

    public static final String SETTINGS = "settings";

    private CacheConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
