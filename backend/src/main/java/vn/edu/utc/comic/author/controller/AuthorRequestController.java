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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.author.dto.AuthorRequestForm;
import vn.edu.utc.comic.author.enums.IntendedStoryType;
import vn.edu.utc.comic.author.service.AuthorRequestService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.common.web.FormErrors;

/**
 * Trang đăng ký làm tác giả của độc giả: form gửi yêu cầu, hoặc tình trạng của yêu cầu đã gửi.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.AUTHOR_REQUEST_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_USER)
public class AuthorRequestController {

    private static final String ATTR_STATUS = "status";
    private static final String ATTR_INTENDED_TYPES = "intendedTypes";

    private final AuthorRequestService authorRequestService;
    private final FlashMessages flash;

    /** Tình trạng đăng ký hiện tại kèm form gửi yêu cầu (form chỉ hiện khi được phép gửi). */
    @GetMapping
    public String showAuthorRequest(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, new AuthorRequestForm());
        return showPage(principal, model);
    }

    /**
     * Gửi yêu cầu. Lỗi gắn được vào ô nhập (bút danh trùng) hiện lại ngay trên form; lỗi về điều kiện gửi
     * (đang có yêu cầu chờ, chưa hết thời gian chờ) hiện ở thông báo flash.
     */
    @PostMapping
    public String submitAuthorRequest(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @Valid @ModelAttribute(ViewConstants.ATTR_FORM) AuthorRequestForm form,
                                      BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return showPage(principal, model);
        }
        try {
            authorRequestService.submit(principal.getId(), form);
            flash.success(redirect, MessageKeys.FLASH_AUTHOR_REQUEST_SUBMITTED);
        } catch (FieldValidationException exception) {
            FormErrors.apply(bindingResult, exception);
            return showPage(principal, model);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_AUTHOR_REQUEST;
    }

    private String showPage(AppUserPrincipal principal, Model model) {
        model.addAttribute(ATTR_STATUS, authorRequestService.getStatus(principal.getId()));
        model.addAttribute(ATTR_INTENDED_TYPES, IntendedStoryType.values());
        return ViewConstants.ME_AUTHOR_REQUEST;
    }
}
