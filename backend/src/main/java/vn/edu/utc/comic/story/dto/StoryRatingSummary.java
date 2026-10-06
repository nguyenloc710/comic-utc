package vn.edu.utc.comic.story.dto;

/**
 * Tổng điểm đánh giá hiện tại của một truyện, đọc lại từ cơ sở dữ liệu sau khi cộng dồn.
 */
public record StoryRatingSummary(int ratingSum, int ratingCount) {

    /** Điểm trung bình; {@code null} khi chưa có lượt đánh giá nào. */
    public Double average() {
        return ratingCount == 0 ? null : (double) ratingSum / ratingCount;
    }
}
