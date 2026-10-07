package vn.edu.utc.comic.interaction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.interaction.dto.AdminCommentFilterRequest;
import vn.edu.utc.comic.interaction.enums.CommentStatus;
import vn.edu.utc.comic.interaction.service.CommentModerationService;

/**
 * Kiểm duyệt bình luận: danh sách có lọc, ẩn kèm lý do, gỡ ẩn. Form ẩn nằm ngay trên từng dòng nên lỗi
 * (thiếu lý do) báo ở thông báo flash thay vì dưới ô nhập.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.ADMIN_COMMENTS_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminCommentController {

    private static final String ATTR_STATUSES = "statuses";

    private final CommentModerationService commentModerationService;
    private final FlashMessages flash;

    /** Danh sách bình luận mới nhất trước; bộ lọc nằm trên query string. */
    @GetMapping
    public String listComments(@ModelAttribute(ViewConstants.ATTR_FILTER) AdminCommentFilterRequest filter,
                               @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, commentModerationService.searchComments(filter, page));
        model.addAttribute(ATTR_STATUSES, CommentStatus.values());
        return ViewConstants.ADMIN_COMMENTS;
    }

    /** Ẩn bình luận rồi quay lại danh sách (PRG). */
    @PostMapping("/{id}/hide")
    public String hideComment(@PathVariable Long id, @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ReasonForm form,
                              BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            flash.error(redirect, new ApiException(ErrorCode.REASON_REQUIRED));
            return ViewConstants.REDIRECT_ADMIN_COMMENTS;
        }
        try {
            commentModerationService.hideComment(id, form.getReason());
            flash.success(redirect, MessageKeys.FLASH_COMMENT_HIDDEN);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_COMMENTS;
    }

    /** Gỡ ẩn bình luận rồi quay lại danh sách (PRG). */
    @PostMapping("/{id}/unhide")
    public String unhideComment(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            commentModerationService.unhideComment(id);
            flash.success(redirect, MessageKeys.FLASH_COMMENT_UNHIDDEN);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_COMMENTS;
    }
}
