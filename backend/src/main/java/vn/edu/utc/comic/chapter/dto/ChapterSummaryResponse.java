package vn.edu.utc.comic.chapter.dto;

import java.time.Instant;

/** Một dòng trong danh sách chương ở trang chi tiết truyện. */
public record ChapterSummaryResponse(int chapterNo, String title, Instant publishedAt, long viewCount) {
}
