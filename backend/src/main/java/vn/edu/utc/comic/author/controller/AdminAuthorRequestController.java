package vn.edu.utc.comic.author.controller;

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
import vn.edu.utc.comic.author.dto.AuthorRequestRejectForm;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;
import vn.edu.utc.comic.author.service.AuthorRequestService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.web.FlashMessages;

/**
 * Màn duyệt yêu cầu đăng ký tác giả của quản trị viên.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.ADMIN_AUTHOR_REQUESTS_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminAuthorRequestController {

    private static final String ATTR_STATUS = "status";
    private static final String ATTR_STATUSES = "statuses";
    private static final String ATTR_REQUEST = "authorRequest";

    private final AuthorRequestService authorRequestService;
    private final FlashMessages flash;

    /** Danh sách yêu cầu theo trạng thái; mặc định là các yêu cầu đang chờ duyệt. */
    @GetMapping
    public String listRequests(@RequestParam(defaultValue = "PENDING") AuthorRequestStatus status,
                               @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, authorRequestService.searchRequests(status, page));
        model.addAttribute(ATTR_STATUS, status);
        model.addAttribute(ATTR_STATUSES, AuthorRequestStatus.values());
        return ViewConstants.ADMIN_AUTHOR_REQUESTS;
    }

    /** Chi tiết một yêu cầu kèm nút duyệt và form từ chối. */
    @GetMapping("/{id}")
    public String showRequest(@PathVariable Long id, Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, new AuthorRequestRejectForm());
        return showDetail(id, model);
    }

    /** Duyệt yêu cầu rồi quay lại danh sách (PRG); lỗi nghiệp vụ hiện ở thông báo flash. */
    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                                 RedirectAttributes redirect) {
        try {
            flash.success(redirect, MessageKeys.FLASH_AUTHOR_REQUEST_APPROVED,
                    authorRequestService.approve(id, principal.getId()));
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_AUTHOR_REQUESTS;
    }

    /** Từ chối yêu cầu; thiếu lý do thì hiện lại trang chi tiết kèm lỗi ở ô nhập. */
    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                                @Valid @ModelAttribute(ViewConstants.ATTR_FORM) AuthorRequestRejectForm form,
                                BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return showDetail(id, model);
        }
        try {
            authorRequestService.reject(id, principal.getId(), form.getRejectReason());
            flash.success(redirect, MessageKeys.FLASH_AUTHOR_REQUEST_REJECTED);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_AUTHOR_REQUESTS;
    }

    private String showDetail(Long requestId, Model model) {
        model.addAttribute(ATTR_REQUEST, authorRequestService.getRequest(requestId));
        return ViewConstants.ADMIN_AUTHOR_REQUEST_DETAIL;
    }
}
