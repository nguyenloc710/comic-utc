package vn.edu.utc.comic.story.dto;

import java.time.Instant;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;

/**
 * Một truyện trong khu vực tác giả: thông tin quản lý kèm các con số thống kê.
 *
 * @param hiddenReason  lý do quản trị viên ẩn truyện; {@code null} khi không bị ẩn
 * @param chapterCount  số chương đã đăng
 * @param ratingAverage điểm trung bình; {@code null} khi chưa có lượt đánh giá nào
 */
public record StudioStoryResponse(
        Long id,
        String slug,
        String title,
        String coverUrl,
        StoryType type,
        StoryStatus status,
        StoryVisibility visibility,
        String hiddenReason,
        int chapterCount,
        long viewCount,
        int followCount,
        int commentCount,
        Double ratingAverage,
        int ratingCount,
        Instant lastChapterAt,
        Instant createdAt) {
}
