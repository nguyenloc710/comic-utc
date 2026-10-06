package vn.edu.utc.comic.author.enums;

/** Trạng thái yêu cầu đăng ký làm tác giả. Mỗi người tối đa một yêu cầu PENDING tại một thời điểm. */
public enum AuthorRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
