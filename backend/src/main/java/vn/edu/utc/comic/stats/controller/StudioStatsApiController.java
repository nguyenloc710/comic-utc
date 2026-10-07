package vn.edu.utc.comic.stats.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.stats.dto.DailyViewPoint;
import vn.edu.utc.comic.stats.service.AuthorStatsService;

/**
 * API dữ liệu biểu đồ của trang thống kê tác giả.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_STUDIO_STATS_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_AUTHOR)
@Tag(name = "Thống kê tác giả", description = "Dữ liệu biểu đồ cho trang thống kê của tác giả")
public class StudioStatsApiController {

    private final AuthorStatsService authorStatsService;

    @GetMapping("/daily-views")
    @Operation(summary = "Lượt xem theo ngày trong 30 ngày gần nhất",
            description = "Mỗi ngày một điểm, ngày không có lượt xem có giá trị 0. Bỏ storyId để cộng mọi truyện "
                    + "của tác giả. Lỗi: 404 STORY_NOT_FOUND nếu truyện không phải của người gọi.")
    public ApiResponse<List<DailyViewPoint>> getDailyViews(
            @RequestParam(required = false) Long storyId, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(authorStatsService.getDailyViews(principal.getId(), storyId));
    }
}
