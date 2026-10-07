package vn.edu.utc.comic.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.stats.service.AuthorStatsService;

/** Trang tổng quan và trang thống kê của khu vực tác giả. */
@Controller
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_AUTHOR)
public class StudioDashboardController {

    private static final String ATTR_OVERVIEW = "overview";
    private static final String ATTR_STORIES = "stories";
    private static final String ATTR_TOP_CHAPTERS = "topChapters";

    private final AuthorStatsService authorStatsService;

    /** Trang tổng quan: các con số chính của tác giả đang đăng nhập và lối tắt tới việc hay làm. */
    @GetMapping(ApiConstants.STUDIO_ROOT)
    public String showDashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ATTR_OVERVIEW, authorStatsService.getOverview(principal.getId()));
        return ViewConstants.STUDIO_DASHBOARD;
    }

    /**
     * Trang thống kê: tổng số liệu, số liệu từng truyện, chương được xem nhiều nhất. Biểu đồ lượt xem theo ngày
     * được trình duyệt tải riêng qua API để đổi truyện không phải tải lại trang.
     */
    @GetMapping(ApiConstants.STUDIO_STATS_PATH)
    public String showStats(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ATTR_OVERVIEW, authorStatsService.getOverview(principal.getId()));
        model.addAttribute(ATTR_STORIES, authorStatsService.findStoryStats(principal.getId()));
        model.addAttribute(ATTR_TOP_CHAPTERS, authorStatsService.findTopChapters(principal.getId()));
        return ViewConstants.STUDIO_STATS;
    }
}
