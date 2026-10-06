package vn.edu.utc.comic.chapter.event;

/**
 * Một người vừa mở đọc một chương đang công khai.
 *
 * @param visitorKey định danh người xem để khử trùng lặp lượt xem: tài khoản nếu đã đăng nhập, phiên nếu là khách
 */
public record ChapterViewedEvent(Long storyId, Long chapterId, String visitorKey) {
}
