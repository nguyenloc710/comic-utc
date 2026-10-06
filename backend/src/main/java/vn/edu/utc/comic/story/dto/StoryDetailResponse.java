package vn.edu.utc.comic.story.dto;

import java.time.Instant;
import java.util.List;
import vn.edu.utc.comic.genre.dto.GenreTagResponse;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;

/**
 * Trang chi tiết của một truyện.
 *
 * @param description text thuần (không phải HTML)
 * @param authorName  bút danh của tác giả
 * @param visibility  khác PUBLISHED nghĩa là người xem đang xem trước nội dung chưa công khai
 *                    (chỉ tác giả của truyện và quản trị viên mới nhận được)
 */
public record StoryDetailResponse(
        Long id,
        String slug,
        String title,
        String altTitle,
        String description,
        String coverUrl,
        StoryType type,
        StoryStatus status,
        StoryVisibility visibility,
        Long authorId,
        String authorName,
        List<GenreTagResponse> genres,
        int chapterCount,
        long viewCount,
        int followCount,
        Double ratingAverage,
        int ratingCount,
        int commentCount,
        Instant lastChapterAt) {

    public boolean isPreview() {
        return visibility != StoryVisibility.PUBLISHED;
    }
}
