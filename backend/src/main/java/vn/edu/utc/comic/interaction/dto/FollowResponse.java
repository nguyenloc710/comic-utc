package vn.edu.utc.comic.interaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trạng thái theo dõi sau thao tác")
public record FollowResponse(
        @Schema(description = "Người gọi có đang theo dõi truyện không") boolean following,
        @Schema(description = "Tổng số người theo dõi truyện") int followCount) {
}
