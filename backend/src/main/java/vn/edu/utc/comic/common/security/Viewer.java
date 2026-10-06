package vn.edu.utc.comic.common.security;

import java.util.Objects;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Người đang xem nội dung, ở dạng mà tầng service dùng được mà không phụ thuộc Spring Security.
 * Khách vãng lai là một Viewer không có id, nhờ vậy service không phải xử lý {@code null}.
 *
 * @param userId id tài khoản; {@code null} với khách vãng lai
 * @param role   vai trò; {@code null} với khách vãng lai
 */
public record Viewer(Long userId, Role role) {

    private static final Viewer ANONYMOUS = new Viewer(null, null);

    public static Viewer anonymous() {
        return ANONYMOUS;
    }

    /** @param principal người đang đăng nhập; {@code null} nghĩa là khách vãng lai */
    public static Viewer of(AppUserPrincipal principal) {
        return principal == null ? ANONYMOUS : new Viewer(principal.getId(), principal.getRole());
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Người xem có đúng là tài khoản mang id này không (khách vãng lai thì luôn không). */
    public boolean isUser(Long otherUserId) {
        return userId != null && Objects.equals(userId, otherUserId);
    }
}
