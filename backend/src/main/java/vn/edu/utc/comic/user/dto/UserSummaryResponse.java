package vn.edu.utc.comic.user.dto;

import java.time.Instant;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

/**
 * Một dòng trong danh sách tài khoản ở trang quản trị. Không bao giờ chứa mật khẩu băm.
 *
 * @param temporarilyLocked đang bị khóa tạm do nhập sai mật khẩu nhiều lần (khác với status BANNED)
 */
public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        String displayName,
        Role role,
        UserStatus status,
        boolean temporarilyLocked,
        Instant lastLoginAt,
        Instant createdAt) {
}
