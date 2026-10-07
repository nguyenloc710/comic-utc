package vn.edu.utc.comic.stats.dto;

/** Một chương trong bảng "chương được xem nhiều nhất" của tác giả. */
public record TopChapterResponse(
        String storyTitle,
        String storySlug,
        int chapterNo,
        String chapterTitle,
        long viewCount) {
}
