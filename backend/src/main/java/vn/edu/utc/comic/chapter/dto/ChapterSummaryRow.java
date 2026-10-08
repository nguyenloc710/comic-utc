package vn.edu.utc.comic.chapter.dto;

import java.time.Instant;

/** Một chương đã đăng kèm id truyện, để gom "các chương mới nhất" của nhiều truyện bằng một truy vấn. */
public record ChapterSummaryRow(Long storyId, int chapterNo, String title, Instant publishedAt, long viewCount) {
}
