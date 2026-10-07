package vn.edu.utc.comic.chapter.dto;

import java.time.Instant;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;

/**
 * Một chương trong khu vực tác giả (mọi trạng thái, khác với danh sách chương phía người đọc).
 *
 * @param scheduledAt  giờ hẹn đăng; chỉ có khi chương đang hẹn giờ
 * @param hiddenReason lý do quản trị viên ẩn chương; {@code null} khi không bị ẩn
 * @param pageCount    số ảnh (truyện tranh)
 * @param wordCount    số từ (truyện chữ)
 */
public record StudioChapterResponse(
        Long id,
        Long storyId,
        int chapterNo,
        String title,
        ChapterStatus status,
        Instant scheduledAt,
        Instant publishedAt,
        String hiddenReason,
        int pageCount,
        int wordCount,
        long viewCount) {

    /** Chương đã từng đăng thì không đổi được số (link và lịch sử đọc của độc giả đang trỏ tới số đó). */
    public boolean numberLocked() {
        return status.hasBeenPublished();
    }
}
