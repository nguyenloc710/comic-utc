package vn.edu.utc.comic.interaction.dto;

import java.time.Instant;
import vn.edu.utc.comic.story.dto.StoryCardResponse;

/**
 * Một truyện trong lịch sử đọc.
 *
 * @param chapterNo chương đang đọc dở, để dựng nút "Đọc tiếp"
 */
public record ReadingHistoryItemResponse(StoryCardResponse story, int chapterNo, Instant readAt) {
}
