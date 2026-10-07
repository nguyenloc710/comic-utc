package vn.edu.utc.comic.interaction.dto;

import java.time.Instant;
import vn.edu.utc.comic.interaction.enums.CommentStatus;

/**
 * Một bình luận trong màn kiểm duyệt: quản trị viên đọc được cả nội dung đã bị ẩn.
 *
 * @param chapterNo số chương nếu bình luận ở trang chương; {@code null} nếu ở trang truyện
 */
public record AdminCommentResponse(
        Long id,
        String content,
        CommentStatus status,
        String hiddenReason,
        Long authorId,
        String authorUsername,
        String storyTitle,
        String storySlug,
        Integer chapterNo,
        Instant createdAt) {
}
