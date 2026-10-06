package vn.edu.utc.comic.interaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import vn.edu.utc.comic.interaction.enums.CommentStatus;

@Schema(description = "Một bình luận kèm các câu trả lời của nó")
public record CommentResponse(
        Long id,
        @Schema(description = "Bình luận gốc mà bình luận này trả lời; null nếu là bình luận gốc") Long parentId,
        String authorName,
        @Schema(description = "URL ảnh đại diện của người viết; null nếu chưa đặt ảnh") String authorAvatarUrl,
        @Schema(description = "Nội dung text thuần; null khi bình luận đã bị ẩn hoặc đã xóa") String content,
        CommentStatus status,
        Instant createdAt,
        @Schema(description = "Bình luận do chính người gọi viết (được phép xóa)") boolean mine,
        @Schema(description = "Các câu trả lời, cũ nhất trước; luôn rỗng với một câu trả lời") List<CommentResponse> replies) {
}
