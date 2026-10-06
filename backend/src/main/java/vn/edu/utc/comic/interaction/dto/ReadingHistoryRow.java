package vn.edu.utc.comic.interaction.dto;

import java.time.Instant;
import vn.edu.utc.comic.story.entity.Story;

/** Kết quả truy vấn lịch sử đọc: truyện, chương đang đọc dở và lần đọc gần nhất. Chỉ dùng trong tầng service. */
public record ReadingHistoryRow(Story story, int chapterNo, Instant readAt) {
}
