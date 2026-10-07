package vn.edu.utc.comic.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.notification.service.NotificationService;

/**
 * Trang thông báo của người đang đăng nhập.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.NOTIFICATIONS_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_USER)
public class NotificationController {

    private final NotificationService notificationService;
    private final FlashMessages flash;

    /** Danh sách thông báo, mới nhất trước. */
    @GetMapping
    public String listNotifications(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, notificationService.findNotifications(principal.getId(), page));
        return ViewConstants.ME_NOTIFICATIONS;
    }

    /**
     * Mở một thông báo: đánh dấu đã đọc rồi chuyển tới nơi thông báo nói đến. Dùng POST vì thao tác này đổi
     * dữ liệu — một link GET sẽ bị trình duyệt tải trước hoặc bị trang khác kích hoạt.
     */
    @PostMapping("/{id}/open")
    public String openNotification(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ViewConstants.redirectTo(notificationService.openNotification(id, principal.getId()));
    }

    /** Đánh dấu mọi thông báo là đã đọc rồi quay lại danh sách (PRG). */
    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal AppUserPrincipal principal, RedirectAttributes redirect) {
        notificationService.markAllRead(principal.getId());
        flash.success(redirect, MessageKeys.FLASH_NOTIFICATIONS_READ);
        return ViewConstants.REDIRECT_NOTIFICATIONS;
    }
}
