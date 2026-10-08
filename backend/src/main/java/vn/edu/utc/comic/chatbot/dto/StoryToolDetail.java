package vn.edu.utc.comic.chatbot.dto;

import java.time.Instant;
import java.util.List;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;

/** Chi tiết một truyện cho hàm "xem truyện": mô tả dài hơn, tác giả, ngày cập nhật. */
public record StoryToolDetail(
        Long id,
        String title,
        String altTitle,
        String authorName,
        StoryType type,
        StoryStatus status,
        List<String> genres,
        int chapterCount,
        Double ratingAverage,
        int ratingCount,
        long viewCount,
        int followCount,
        Instant lastChapterAt,
        String description) {
}
