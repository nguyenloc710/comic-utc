package vn.edu.utc.comic.stats.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;

/** Trang tổng quan của khu vực tác giả. Số liệu truyện và biểu đồ được bổ sung ở giai đoạn 4. */
@Controller
@RequestMapping(ApiConstants.STUDIO_ROOT)
@PreAuthorize(SecurityConstants.HAS_ROLE_AUTHOR)
public class StudioDashboardController {

    /** Hiển thị trang tổng quan của tác giả đang đăng nhập. */
    @GetMapping
    public String showDashboard() {
        return ViewConstants.STUDIO_DASHBOARD;
    }
}
