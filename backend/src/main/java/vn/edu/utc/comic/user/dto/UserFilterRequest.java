package vn.edu.utc.comic.user.dto;

import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

/**
 * Bộ lọc danh sách tài khoản ở trang quản trị. Trường để trống nghĩa là không lọc theo trường đó.
 *
 * @param keyword một phần tên đăng nhập, email hoặc tên hiển thị
 */
public record UserFilterRequest(String keyword, Role role, UserStatus status) {
}
