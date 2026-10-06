package vn.edu.utc.comic.interaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Điểm đánh giá của truyện sau thao tác")
public record RatingResponse(
        @Schema(description = "Số sao người gọi vừa chấm") int myStars,
        @Schema(description = "Điểm trung bình của truyện; null khi chưa có lượt đánh giá nào") Double ratingAverage,
        @Schema(description = "Tổng số lượt đánh giá") int ratingCount) {
}
