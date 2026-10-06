package vn.edu.utc.comic.interaction.dto;

/**
 * Quan hệ của người đang xem với một truyện, để trang chi tiết hiện đúng trạng thái các nút.
 *
 * @param following         đang theo dõi truyện
 * @param myStars           số sao người xem đã chấm; {@code null} nếu chưa đánh giá
 * @param continueChapterNo chương đang đọc dở; {@code null} nếu chưa đọc chương nào
 */
public record StoryViewerStateResponse(boolean following, Integer myStars, Integer continueChapterNo) {

    /** Trạng thái của khách vãng lai. */
    public static StoryViewerStateResponse none() {
        return new StoryViewerStateResponse(false, null, null);
    }
}
