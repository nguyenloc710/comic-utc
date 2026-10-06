package vn.edu.utc.comic.interaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.utc.comic.common.constant.StoryConstants;

@Schema(description = "Bình luận mới")
public record CommentCreateRequest(
        @Schema(description = "Truyện được bình luận")
        @NotNull(message = "{validation.comment.story.required}")
        Long storyId,

        @Schema(description = "Chương được bình luận; bỏ trống nếu bình luận ở trang truyện")
        Long chapterId,

        @Schema(description = "Bình luận được trả lời; bỏ trống nếu là bình luận gốc")
        Long parentId,

        @Schema(description = "Nội dung dạng text thuần")
        @NotBlank(message = "{validation.comment.content.required}")
        @Size(max = StoryConstants.COMMENT_MAX_LENGTH, message = "{validation.comment.content.size}")
        String content) {
}
