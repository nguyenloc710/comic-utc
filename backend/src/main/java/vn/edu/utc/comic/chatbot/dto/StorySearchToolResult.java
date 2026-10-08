package vn.edu.utc.comic.chatbot.dto;

import java.util.List;

/**
 * Kết quả hàm tìm truyện.
 *
 * @param totalMatches   tổng số truyện khớp (có thể nhiều hơn số truyện trả về)
 * @param relaxedFilters các điều kiện máy chủ đã phải bỏ để có kết quả (keyword, chapterRange, status); mô hình
 *                       phải nói rõ với người dùng thay vì giả vờ là khớp hoàn toàn
 */
public record StorySearchToolResult(List<StoryToolItem> items, long totalMatches, List<String> relaxedFilters) {

    public List<Long> storyIds() {
        return items.stream().map(StoryToolItem::id).toList();
    }
}
