package vn.edu.utc.comic.auth.controller;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.auth.dto.ChangePasswordForm;
import vn.edu.utc.comic.auth.dto.ProfileForm;
import vn.edu.utc.comic.auth.service.ProfileService;
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
 * Hồ sơ cá nhân và đổi mật khẩu của người đang đăng nhập.
 * Mọi thao tác đều lấy id tài khoản từ phiên đăng nhập, không nhận từ request.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_USER)
public class ProfileController {

    private static final String ATTR_PROFILE = "profile";
    private static final String FIELD_AVATAR = "avatar";

    private final ProfileService profileService;
    private final FlashMessages flash;

    /** Trang hồ sơ kèm form sửa điền sẵn giá trị hiện tại. */
    @GetMapping(ApiConstants.PROFILE_PATH)
    public String showProfile(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ATTR_PROFILE, profileService.getProfile(principal.getId()));
        model.addAttribute(ViewConstants.ATTR_FORM, profileService.getProfileForm(principal.getId()));
        return ViewConstants.ME_PROFILE;
    }

    /** Lưu hồ sơ; ảnh không hợp lệ thì hiện lỗi ngay dưới ô chọn ảnh và không lưu gì. */
    @PostMapping(ApiConstants.PROFILE_PATH)
    public String updateProfile(@AuthenticationPrincipal AppUserPrincipal principal,
                                @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ProfileForm form,
                                BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (!bindingResult.hasErrors()) {
            try {
                profileService.updateProfile(principal.getId(), form);
            } catch (ApiException exception) {
                FormErrors.apply(bindingResult, FIELD_AVATAR, exception);
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_PROFILE, profileService.getProfile(principal.getId()));
            return ViewConstants.ME_PROFILE;
        }
        flash.success(redirect, MessageKeys.FLASH_PROFILE_SAVED);
        return ViewConstants.REDIRECT_PROFILE;
    }

    /** Form đổi mật khẩu. */
    @GetMapping(ApiConstants.PASSWORD_PATH)
    public String showChangePassword(Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, new ChangePasswordForm());
        return ViewConstants.ME_PASSWORD;
    }

    /** Đổi mật khẩu; lỗi (sai mật khẩu hiện tại, mật khẩu mới yếu, không khớp) hiện ngay ở từng ô. */
    @PostMapping(ApiConstants.PASSWORD_PATH)
    public String changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
                                 @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ChangePasswordForm form,
                                 BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return ViewConstants.ME_PASSWORD;
        }
        try {
            profileService.changePassword(principal.getId(), form);
        } catch (FieldValidationException exception) {
            FormErrors.apply(bindingResult, exception);
            return ViewConstants.ME_PASSWORD;
        }
        flash.success(redirect, MessageKeys.FLASH_PASSWORD_CHANGED);
        return ViewConstants.REDIRECT_PROFILE;
    }
}
