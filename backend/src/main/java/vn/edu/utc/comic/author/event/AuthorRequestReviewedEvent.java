package vn.edu.utc.comic.author.event;

/**
 * Phát khi quản trị viên duyệt hoặc từ chối một yêu cầu đăng ký tác giả, để gửi thông báo cho người gửi.
 *
 * @param userId       người đã gửi yêu cầu
 * @param approved     {@code true} nếu được duyệt
 * @param penName      bút danh trong yêu cầu
 * @param rejectReason lý do từ chối; {@code null} khi được duyệt
 */
public record AuthorRequestReviewedEvent(Long userId, boolean approved, String penName, String rejectReason) {
}
