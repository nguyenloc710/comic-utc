package vn.edu.utc.comic.common.security;

/**
 * Phát khi vai trò, trạng thái, tên hiển thị hoặc ảnh đại diện của một tài khoản vừa đổi.
 * Service nào sửa các trường đó đều phải phát sự kiện này để phiên đang đăng nhập của tài khoản được làm mới.
 */
public record AccountChangedEvent(Long userId) {
}
