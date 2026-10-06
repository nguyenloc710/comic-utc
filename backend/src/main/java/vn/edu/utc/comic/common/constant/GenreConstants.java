package vn.edu.utc.comic.common.constant;

/**
 * Giới hạn cố định của dữ liệu thể loại (khớp độ dài cột trong lược đồ).
 */
public final class GenreConstants {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int SLUG_MAX_LENGTH = 120;
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    public static final int SORT_ORDER_MAX = 9999;

    private GenreConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
