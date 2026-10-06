package vn.edu.utc.comic.user.enums;

/**
 * Vai trò của tài khoản. Mỗi tài khoản có đúng một vai trò; AUTHOR và ADMIN kế thừa quyền của USER
 * qua RoleHierarchy (xem SecurityConfig). Tên giá trị phải trùng các hằng ROLE_* trong SecurityConstants.
 */
public enum Role {
    USER,
    AUTHOR,
    ADMIN
}
