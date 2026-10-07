package vn.edu.utc.comic.chapter.event;

/**
 * Phát khi một chương vừa chuyển sang trạng thái đã đăng (tác giả bấm "Đăng ngay" hoặc job đăng theo giờ hẹn).
 * Gỡ ẩn một chương KHÔNG phát sự kiện này, nên người theo dõi không nhận lại thông báo chương mới.
 *
 * @param storyPublic truyện có đang công khai tại thời điểm đăng không; chương của truyện nháp hoặc đang bị ẩn
 *                    không được báo cho ai
 */
public record ChapterPublishedEvent(
        Long storyId,
        String storyTitle,
        String storySlug,
        Long chapterId,
        int chapterNo,
        boolean storyPublic) {
}
