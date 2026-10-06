package vn.edu.utc.comic.story.dto;

import java.time.Instant;
import java.util.List;
import vn.edu.utc.comic.genre.dto.GenreTagResponse;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Một truyện ở dạng thẻ trong danh sách (trang chủ, tìm kiếm, bảng xếp hạng, tủ truyện).
 *
 * @param coverUrl      URL ảnh bìa đã dựng sẵn; {@code null} khi chưa có bìa
 * @param chapterCount  số chương đã đăng
 * @param ratingAverage điểm trung bình; {@code null} khi chưa có lượt đánh giá nào
 */
public record StoryCardResponse(
        Long id,
        String slug,
        String title,
        String coverUrl,
        StoryType type,
        StoryStatus status,
        List<GenreTagResponse> genres,
        int chapterCount,
        long viewCount,
        int followCount,
        Double ratingAverage,
        int ratingCount,
        Instant lastChapterAt) {
}
