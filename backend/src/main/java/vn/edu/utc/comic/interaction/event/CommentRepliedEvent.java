package vn.edu.utc.comic.interaction.event;

/**
 * Phát khi một bình luận được người KHÁC trả lời.
 *
 * @param recipientId người viết bình luận gốc, là người nhận thông báo
 * @param replierName tên hiển thị của người trả lời
 * @param link        đường dẫn tới nơi có bình luận (trang truyện hoặc trang chương)
 */
public record CommentRepliedEvent(Long recipientId, String replierName, String storyTitle, String link) {
}
