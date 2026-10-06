package vn.edu.utc.comic.stats.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;

/** Trang tổng quan của khu vực quản trị. Chỉ số và biểu đồ hệ thống được bổ sung ở giai đoạn 5. */
@Controller
@RequestMapping(ApiConstants.ADMIN_ROOT)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminDashboardController {

    /** Hiển thị trang tổng quan quản trị. */
    @GetMapping
    public String showDashboard() {
        return ViewConstants.ADMIN_DASHBOARD;
    }
}
