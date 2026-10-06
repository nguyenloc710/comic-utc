package vn.edu.utc.comic.common.constant;

/**
 * Hằng số nghiệp vụ của truyện và tương tác của độc giả. Chỉ chứa giá trị CỐ ĐỊNH;
 * tham số có thể đổi khi vận hành nằm ở bảng setting.
 */
public final class StoryConstants {

    public static final int RATING_MIN_STARS = 1;
    public static final int RATING_MAX_STARS = 5;

    /** Khớp độ dài cột comment.content. */
    public static final int COMMENT_MAX_LENGTH = 1000;

    /** Dùng khi bảng setting chưa có khóa tương ứng. */
    public static final int DEFAULT_COMMENT_COOLDOWN_SECONDS = 15;
    public static final int DEFAULT_VIEW_DEDUPE_MINUTES = 30;
    public static final int DEFAULT_RANKING_MIN_RATING_COUNT = 5;

    private StoryConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
