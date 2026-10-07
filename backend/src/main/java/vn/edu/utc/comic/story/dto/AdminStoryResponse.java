package vn.edu.utc.comic.story.dto;

import java.time.Instant;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;

/**
 * Một truyện trong màn quản trị, kể cả truyện nháp, bị ẩn hay đã xóa.
 *
 * @param authorUsername tên đăng nhập của tác giả (để quản trị viên tra sang trang người dùng)
 * @param deletedAt      khác {@code null} nếu tác giả đã xóa mềm
 */
public record AdminStoryResponse(
        Long id,
        String slug,
        String title,
        String coverUrl,
        StoryType type,
        StoryStatus status,
        StoryVisibility visibility,
        String hiddenReason,
        Long authorId,
        String authorUsername,
        String authorDisplayName,
        int chapterCount,
        long viewCount,
        int followCount,
        int commentCount,
        Instant deletedAt,
        Instant createdAt) {
}
