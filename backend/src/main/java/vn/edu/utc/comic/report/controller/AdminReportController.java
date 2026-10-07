package vn.edu.utc.comic.report.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.dto.ReasonForm;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.report.dto.ReportResponse;
import vn.edu.utc.comic.report.enums.ReportStatus;
import vn.edu.utc.comic.report.service.ReportService;

/**
 * Hàng đợi báo cáo vi phạm của quản trị viên: xem, bỏ qua, hoặc ẩn nội dung bị báo cáo.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.ADMIN_REPORTS_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminReportController {

    private static final String ATTR_STATUS = "status";
    private static final String ATTR_STATUSES = "statuses";
    private static final String ATTR_REPORT = "report";
    private static final String PARAM_NOTE = "note";

    private final ReportService reportService;
    private final FlashMessages flash;

    /** Danh sách báo cáo theo trạng thái; mặc định là các báo cáo đang chờ. */
    @GetMapping
    public String listReports(@RequestParam(defaultValue = "PENDING") ReportStatus status,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, reportService.searchReports(status, page));
        model.addAttribute(ATTR_STATUS, status);
        model.addAttribute(ATTR_STATUSES, ReportStatus.values());
        return ViewConstants.ADMIN_REPORTS;
    }

    /** Chi tiết một báo cáo kèm form bỏ qua và form ẩn nội dung. */
    @GetMapping("/{id}")
    public String showReport(@PathVariable Long id, Model model) {
        ReportResponse report = reportService.getReport(id);
        // Điền sẵn lý do ẩn bằng mô tả của người báo cáo (nếu có) để quản trị viên chỉ việc sửa lại
        ReasonForm form = new ReasonForm();
        form.setReason(report.detail());
        model.addAttribute(ViewConstants.ATTR_FORM, form);
        model.addAttribute(ATTR_REPORT, report);
        return ViewConstants.ADMIN_REPORT_DETAIL;
    }

    /** Bỏ qua báo cáo (ghi chú tùy chọn) rồi quay lại hàng đợi. */
    @PostMapping("/{id}/dismiss")
    public String dismissReport(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                                @RequestParam(name = PARAM_NOTE, required = false) String note,
                                RedirectAttributes redirect) {
        try {
            reportService.dismiss(id, principal.getId(), note);
            flash.success(redirect, MessageKeys.FLASH_REPORT_DISMISSED);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_REPORTS;
    }

    /** Ẩn nội dung bị báo cáo với lý do bắt buộc rồi đóng báo cáo; thiếu lý do thì hiện lại trang chi tiết. */
    @PostMapping("/{id}/resolve")
    public String resolveReport(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                                @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ReasonForm form,
                                BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return showDetail(id, model);
        }
        try {
            reportService.resolve(id, principal.getId(), form.getReason());
            flash.success(redirect, MessageKeys.FLASH_REPORT_RESOLVED);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_REPORTS;
    }

    private String showDetail(Long reportId, Model model) {
        model.addAttribute(ATTR_REPORT, reportService.getReport(reportId));
        return ViewConstants.ADMIN_REPORT_DETAIL;
    }
}
