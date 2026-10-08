package vn.edu.utc.comic.chatbot.dto;

/**
 * Kết quả hàm xem chi tiết truyện. Trả {@code found = false} thay vì ném lỗi để mô hình hiểu là id không có
 * (hoặc truyện không còn công khai) và tự xử lý tiếp.
 */
public record StoryDetailToolResult(boolean found, StoryToolDetail story) {

    public static StoryDetailToolResult notFound() {
        return new StoryDetailToolResult(false, null);
    }
}
