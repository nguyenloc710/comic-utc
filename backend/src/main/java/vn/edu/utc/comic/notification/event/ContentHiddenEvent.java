package vn.edu.utc.comic.notification.event;

/**
 * Phát khi quản trị viên ẩn một truyện, chương hoặc bình luận, để báo cho người tạo nội dung đó.
 *
 * @param recipientId  tác giả truyện / chương hoặc người viết bình luận
 * @param contentLabel tên ngắn của nội dung bị ẩn (tên truyện, "Chương 3 – Tên truyện", đoạn đầu bình luận)
 * @param reason       lý do quản trị viên nhập
 * @param link         đường dẫn để người nhận xem lại (studio với truyện / chương, trang truyện với bình luận)
 */
public record ContentHiddenEvent(Long recipientId, String contentLabel, String reason, String link) {
}
