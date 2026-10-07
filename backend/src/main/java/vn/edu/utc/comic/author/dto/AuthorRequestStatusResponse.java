package vn.edu.utc.comic.author.dto;

import java.time.Instant;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Tình trạng đăng ký tác giả của một tài khoản, đủ để trang /me/author-request quyết định hiện gì.
 *
 * @param role           vai trò hiện tại (tác giả và quản trị viên không gửi yêu cầu)
 * @param latestRequest  yêu cầu gần nhất; {@code null} nếu chưa từng gửi
 * @param canSubmit      có được gửi yêu cầu mới ngay bây giờ không
 * @param retryAllowedAt thời điểm được gửi lại sau khi bị từ chối; {@code null} nếu không trong thời gian chờ
 */
public record AuthorRequestStatusResponse(
        Role role,
        AuthorRequestResponse latestRequest,
        boolean canSubmit,
        Instant retryAllowedAt) {
}
