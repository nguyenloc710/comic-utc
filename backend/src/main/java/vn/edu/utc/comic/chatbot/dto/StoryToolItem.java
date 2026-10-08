package vn.edu.utc.comic.chatbot.dto;

import java.util.List;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Một truyện trong kết quả hàm, ở dạng gọn để tiết kiệm token.
 *
 * @param genres           slug thể loại
 * @param shortDescription mô tả đã cắt ngắn
 */
public record StoryToolItem(
        Long id,
        String title,
        StoryType type,
        StoryStatus status,
        List<String> genres,
        int chapterCount,
        Double ratingAverage,
        int ratingCount,
        long viewCount,
        String shortDescription) {
}
