package vn.edu.utc.comic.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.stats.service.AdminStatsService;

/** Trang tổng quan của khu vực quản trị: các chỉ số chính; biểu đồ do trình duyệt tải qua API. */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.ADMIN_ROOT)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminDashboardController {

    private static final String ATTR_OVERVIEW = "overview";

    private final AdminStatsService adminStatsService;

    /** Hiển thị trang tổng quan quản trị. */
    @GetMapping
    public String showDashboard(Model model) {
        model.addAttribute(ATTR_OVERVIEW, adminStatsService.getOverview());
        return ViewConstants.ADMIN_DASHBOARD;
    }
}
