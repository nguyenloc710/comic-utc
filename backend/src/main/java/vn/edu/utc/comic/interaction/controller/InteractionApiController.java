package vn.edu.utc.comic.interaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.interaction.dto.FollowResponse;
import vn.edu.utc.comic.interaction.dto.RatingRequest;
import vn.edu.utc.comic.interaction.dto.RatingResponse;
import vn.edu.utc.comic.interaction.service.FollowService;
import vn.edu.utc.comic.interaction.service.RatingService;

/**
 * API theo dõi và đánh giá truyện, gọi bằng fetch() từ trang chi tiết. Mọi thao tác lấy người thực hiện
 * từ phiên đăng nhập.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_STORIES_PATH)
@Tag(name = "Tương tác với truyện", description = "Theo dõi, bỏ theo dõi và đánh giá sao")
public class InteractionApiController {

    private final FollowService followService;
    private final RatingService ratingService;

    @PutMapping("/{storyId}/follow")
    @Operation(summary = "Theo dõi truyện",
            description = "Gọi lặp lại không đổi kết quả. Lỗi: 404 STORY_NOT_FOUND nếu truyện không tồn tại hoặc không công khai.")
    public ApiResponse<FollowResponse> follow(@PathVariable Long storyId,
                                              @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(followService.follow(storyId, principal.getId()));
    }

    @DeleteMapping("/{storyId}/follow")
    @Operation(summary = "Bỏ theo dõi truyện", description = "Gọi lặp lại không đổi kết quả.")
    public ApiResponse<FollowResponse> unfollow(@PathVariable Long storyId,
                                                @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(followService.unfollow(storyId, principal.getId()));
    }

    @PutMapping("/{storyId}/rating")
    @Operation(summary = "Chấm hoặc sửa điểm đánh giá",
            description = "Mỗi người một lượt cho mỗi truyện. Lỗi: 400 VALIDATION_ERROR nếu số sao ngoài 1–5, "
                    + "404 STORY_NOT_FOUND, 409 RATING_OWN_STORY nếu tự đánh giá truyện của mình.")
    public ApiResponse<RatingResponse> rate(@PathVariable Long storyId, @Valid @RequestBody RatingRequest request,
                                            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(ratingService.rate(storyId, principal.getId(), request.stars()));
    }
}
