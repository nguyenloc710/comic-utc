package vn.edu.utc.comic.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.user.dto.UserFilterRequest;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;
import vn.edu.utc.comic.user.service.AdminUserService;

/**
 * Quản trị tài khoản: danh sách có lọc, khóa và mở khóa.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.ADMIN_USERS_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminUserController {

    private static final String ATTR_ROLES = "roles";
    private static final String ATTR_STATUSES = "statuses";

    private final AdminUserService adminUserService;
    private final FlashMessages flash;

    /** Danh sách tài khoản; bộ lọc nằm trên query string để chia sẻ được link. */
    @GetMapping
    public String listUsers(@ModelAttribute(ViewConstants.ATTR_FILTER) UserFilterRequest filter,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, adminUserService.searchUsers(filter, page));
        model.addAttribute(ATTR_ROLES, Role.values());
        model.addAttribute(ATTR_STATUSES, UserStatus.values());
        return ViewConstants.ADMIN_USERS;
    }

    /** Khóa tài khoản rồi quay lại danh sách (PRG); lỗi nghiệp vụ hiện ở thông báo flash. */
    @PostMapping("/{id}/ban")
    public String banUser(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            flash.success(redirect, MessageKeys.FLASH_USER_BANNED, adminUserService.banUser(id));
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_USERS;
    }

    /** Mở khóa tài khoản rồi quay lại danh sách (PRG). */
    @PostMapping("/{id}/unban")
    public String unbanUser(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            flash.success(redirect, MessageKeys.FLASH_USER_UNBANNED, adminUserService.unbanUser(id));
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_USERS;
    }
}
