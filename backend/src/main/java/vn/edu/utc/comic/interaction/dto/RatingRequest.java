package vn.edu.utc.comic.interaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import vn.edu.utc.comic.common.constant.StoryConstants;

@Schema(description = "Đánh giá sao cho một truyện")
public record RatingRequest(
        @Schema(description = "Số sao, từ 1 đến 5", example = "4")
        @NotNull(message = "{validation.rating.stars.range}")
        @Min(value = StoryConstants.RATING_MIN_STARS, message = "{validation.rating.stars.range}")
        @Max(value = StoryConstants.RATING_MAX_STARS, message = "{validation.rating.stars.range}")
        Integer stars) {
}
