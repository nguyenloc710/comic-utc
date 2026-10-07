package vn.edu.utc.comic.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.system.dto.AuditLogFilterRequest;
import vn.edu.utc.comic.system.service.AdminSettingService;
import vn.edu.utc.comic.system.service.AuditLogQueryService;

/**
 * Tham số vận hành và nhật ký kiểm toán của quản trị viên.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminSystemController {

    private static final String ATTR_SETTING_GROUPS = "settingGroups";
    private static final String ATTR_ACTIONS = "actions";
    private static final String PARAM_VALUE = "value";

    private final AdminSettingService adminSettingService;
    private final AuditLogQueryService auditLogQueryService;
    private final FlashMessages flash;

    /** Mọi tham số, gom theo nhóm, mỗi tham số một form sửa. */
    @GetMapping(ApiConstants.ADMIN_SETTINGS_PATH)
    public String listSettings(Model model) {
        model.addAttribute(ATTR_SETTING_GROUPS, adminSettingService.findAllGrouped());
        return ViewConstants.ADMIN_SETTINGS;
    }

    /** Lưu một tham số; giá trị sai kiểu báo ở thông báo flash (mỗi dòng là một form nhỏ, không có ô lỗi riêng). */
    @PostMapping(ApiConstants.ADMIN_SETTINGS_PATH + "/{key}")
    public String updateSetting(@PathVariable String key, @RequestParam(PARAM_VALUE) String value,
                                @AuthenticationPrincipal AppUserPrincipal principal, RedirectAttributes redirect) {
        try {
            adminSettingService.updateSetting(key, value, principal.getId());
            flash.success(redirect, MessageKeys.FLASH_SETTING_SAVED, key);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_SETTINGS;
    }

    /** Nhật ký kiểm toán, lọc theo hành động và người thực hiện. */
    @GetMapping(ApiConstants.ADMIN_AUDIT_LOGS_PATH)
    public String listAuditLogs(@ModelAttribute(ViewConstants.ATTR_FILTER) AuditLogFilterRequest filter,
                                @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, auditLogQueryService.searchLogs(filter, page));
        model.addAttribute(ATTR_ACTIONS, AuditAction.values());
        return ViewConstants.ADMIN_AUDIT_LOGS;
    }
}
