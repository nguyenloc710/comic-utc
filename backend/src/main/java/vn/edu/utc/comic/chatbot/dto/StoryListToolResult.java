package vn.edu.utc.comic.chatbot.dto;

import java.util.List;

/** Kết quả các hàm trả danh sách truyện không có bộ lọc (truyện tương tự, truyện đang hot). */
public record StoryListToolResult(List<StoryToolItem> items) {

    public List<Long> storyIds() {
        return items.stream().map(StoryToolItem::id).toList();
    }
}
