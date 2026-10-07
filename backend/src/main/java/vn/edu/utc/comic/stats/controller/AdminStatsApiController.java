package vn.edu.utc.comic.stats.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.stats.dto.AdminChartsResponse;
import vn.edu.utc.comic.stats.service.AdminStatsService;

/**
 * API dữ liệu biểu đồ của trang tổng quan quản trị.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_ADMIN_STATS_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
@Tag(name = "Thống kê hệ thống", description = "Dữ liệu biểu đồ cho trang tổng quan quản trị")
public class AdminStatsApiController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/charts")
    @Operation(summary = "Dữ liệu bốn biểu đồ của trang tổng quan",
            description = "Đăng ký mới và lượt xem 30 ngày (mỗi ngày một điểm), phân bố thể loại, truyện xem nhiều nhất.")
    public ApiResponse<AdminChartsResponse> getCharts() {
        return ApiResponse.success(adminStatsService.getCharts());
    }
}
