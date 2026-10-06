package vn.edu.utc.comic.auth.dto;

import java.time.Instant;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Thông tin tài khoản hiển thị ở trang hồ sơ.
 *
 * @param avatarUrl URL ảnh đại diện đã dựng sẵn; {@code null} khi chưa đặt ảnh
 * @param createdAt thời điểm đăng ký
 */
public record ProfileResponse(
        String username,
        String email,
        String displayName,
        String avatarUrl,
        Role role,
        Instant createdAt) {
}
