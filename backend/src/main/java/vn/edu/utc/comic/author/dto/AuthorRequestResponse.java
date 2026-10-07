package vn.edu.utc.comic.author.dto;

import java.time.Instant;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;
import vn.edu.utc.comic.author.enums.IntendedStoryType;

/**
 * Một yêu cầu đăng ký tác giả, cho trang của người gửi và màn duyệt của quản trị viên.
 *
 * @param username     tên đăng nhập của người gửi
 * @param rejectReason lý do từ chối; {@code null} khi chưa bị từ chối
 * @param reviewedAt   thời điểm duyệt hoặc từ chối; {@code null} khi còn chờ
 */
public record AuthorRequestResponse(
        Long id,
        Long userId,
        String username,
        String displayName,
        String email,
        String penName,
        String introduction,
        IntendedStoryType intendedType,
        AuthorRequestStatus status,
        String rejectReason,
        Instant reviewedAt,
        Instant createdAt) {
}
