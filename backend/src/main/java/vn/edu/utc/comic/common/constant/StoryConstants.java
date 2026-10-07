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

    /** Khớp độ dài cột story.title / story.alt_title và story.slug. */
    public static final int TITLE_MAX_LENGTH = 200;
    public static final int SLUG_MAX_LENGTH = 220;

    /** Cột description là TEXT; giới hạn này để phần giới thiệu vừa một màn hình đọc. */
    public static final int DESCRIPTION_MAX_LENGTH = 5000;

    /** Số thể loại tối đa của một truyện: gắn quá nhiều thì bộ lọc theo thể loại mất ý nghĩa. */
    public static final int GENRES_MAX = 8;

    /** Dùng khi bảng setting chưa có khóa tương ứng. */
    public static final int DEFAULT_COMMENT_COOLDOWN_SECONDS = 15;
    public static final int DEFAULT_VIEW_DEDUPE_MINUTES = 30;
    public static final int DEFAULT_RANKING_MIN_RATING_COUNT = 5;

    private StoryConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
