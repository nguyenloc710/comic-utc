package vn.edu.utc.comic.stats.dto;

/**
 * Tổng số liệu trên mọi truyện (chưa xóa) của một tác giả.
 *
 * <p>Các tham số là kiểu bao vì SUM trên tập rỗng trả về NULL; constructor đổi NULL thành 0 để template
 * không phải kiểm tra.
 *
 * @param chapterCount tổng số chương đã đăng
 */
public record AuthorStatsOverview(
        Long storyCount,
        Long chapterCount,
        Long viewCount,
        Long followCount,
        Long commentCount,
        Long ratingSum,
        Long ratingCount) {

    public AuthorStatsOverview {
        storyCount = orZero(storyCount);
        chapterCount = orZero(chapterCount);
        viewCount = orZero(viewCount);
        followCount = orZero(followCount);
        commentCount = orZero(commentCount);
        ratingSum = orZero(ratingSum);
        ratingCount = orZero(ratingCount);
    }

    /** Điểm trung bình trên mọi lượt đánh giá của mọi truyện; {@code null} khi chưa có lượt nào. */
    public Double ratingAverage() {
        return ratingCount == 0 ? null : (double) ratingSum / ratingCount;
    }

    private static Long orZero(Long value) {
        return value == null ? Long.valueOf(0) : value;
    }
}
