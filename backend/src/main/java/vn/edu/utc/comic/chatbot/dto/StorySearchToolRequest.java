package vn.edu.utc.comic.chatbot.dto;

import java.util.List;
import org.springframework.ai.tool.annotation.ToolParam;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Tham số của hàm tìm truyện. Mọi trường đều tùy chọn; mô tả trường là văn bản cho mô hình đọc.
 */
public record StorySearchToolRequest(
        @ToolParam(required = false, description = "Slug các thể loại truyện PHẢI có đủ, lấy từ danh sách thể loại trong hướng dẫn")
        List<String> genreSlugs,
        @ToolParam(required = false, description = "Slug các thể loại truyện KHÔNG được có")
        List<String> excludeGenreSlugs,
        @ToolParam(required = false, description = "COMIC = truyện tranh, NOVEL = truyện chữ")
        StoryType type,
        @ToolParam(required = false, description = "ONGOING = đang ra, COMPLETED = đã hoàn thành, PAUSED = tạm ngưng")
        StoryStatus status,
        @ToolParam(required = false, description = "Số chương đã đăng tối thiểu")
        Integer minChapters,
        @ToolParam(required = false, description = "Số chương đã đăng tối đa")
        Integer maxChapters,
        @ToolParam(required = false, description = "Từ khóa tìm trong tên và mô tả truyện; chỉ dùng khi người dùng nêu tên hoặc chi tiết cụ thể")
        String keyword,
        @ToolParam(required = false, description = "Cách sắp xếp: VIEWS = xem nhiều, RATING = điểm cao, NEWEST = mới đăng, UPDATED = mới cập nhật, FOLLOWS = theo dõi nhiều")
        StorySort sortBy,
        @ToolParam(required = false, description = "Số truyện muốn nhận, tối đa 10")
        Integer limit) {
}
