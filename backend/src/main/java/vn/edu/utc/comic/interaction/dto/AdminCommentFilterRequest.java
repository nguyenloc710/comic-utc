package vn.edu.utc.comic.interaction.dto;

import vn.edu.utc.comic.interaction.enums.CommentStatus;

/**
 * Bộ lọc danh sách bình luận ở trang quản trị.
 *
 * @param keyword một phần nội dung bình luận
 */
public record AdminCommentFilterRequest(String keyword, CommentStatus status) {
}
