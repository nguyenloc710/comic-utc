package vn.edu.utc.comic.chatbot.dto;

import java.util.List;

/**
 * Một lần mô hình gọi hàm tra cứu.
 *
 * @param arguments tham số mô hình đã điền (để tóm tắt bộ lọc cho lượt sau và để chẩn đoán)
 * @param storyIds  id các truyện hàm đã trả: chỉ những id này mới được phép xuất hiện trong gợi ý
 */
public record ToolTraceEntry(String tool, Object arguments, List<Long> storyIds) {
}
