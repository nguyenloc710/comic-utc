package vn.edu.utc.comic.story.dto;

import java.util.List;
import vn.edu.utc.comic.chapter.dto.ChapterSummaryResponse;

/**
 * Một truyện trong khối "Truyện mới cập nhật" của trang chủ: thẻ truyện kèm vài chương đăng gần nhất.
 *
 * @param latestChapters chương đã đăng mới nhất trước
 */
public record StoryUpdateResponse(StoryCardResponse story, List<ChapterSummaryResponse> latestChapters) {
}
