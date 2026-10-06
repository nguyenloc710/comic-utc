package vn.edu.utc.comic.auth.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.auth.dto.RegisterForm;
import vn.edu.utc.comic.auth.service.RegistrationService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.security.LoginError;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.common.web.FormErrors;

/**
 * Trang đăng nhập (form do Spring Security xử lý) và tự đăng ký tài khoản.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final String ATTR_LOGIN_ERROR_KEY = "loginErrorKey";

    private final RegistrationService registrationService;
    private final FlashMessages flash;

    /**
     * Trang đăng nhập; đã đăng nhập thì về trang chủ thay vì hiện lại form.
     *
     * @param error mã lỗi do LoginFailureHandler gắn lên URL; mã lạ được coi như sai thông tin đăng nhập
     */
    @GetMapping(ApiConstants.LOGIN_PATH)
    public String showLogin(@AuthenticationPrincipal AppUserPrincipal principal,
                            @RequestParam(name = SecurityConstants.LOGIN_ERROR_PARAM, required = false) String error,
                            Model model) {
        if (principal != null) {
            return ViewConstants.REDIRECT_HOME;
        }
        if (error != null) {
            LoginError loginError = LoginError.fromCode(error).orElse(LoginError.BAD_CREDENTIALS);
            model.addAttribute(ATTR_LOGIN_ERROR_KEY, loginError.getMessageKey());
        }
        return ViewConstants.AUTH_LOGIN;
    }

    /** Form đăng ký; đã đăng nhập thì về trang chủ. */
    @GetMapping(ApiConstants.REGISTER_PATH)
    public String showRegister(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        if (principal != null) {
            return ViewConstants.REDIRECT_HOME;
        }
        model.addAttribute(ViewConstants.ATTR_FORM, new RegisterForm());
        return ViewConstants.AUTH_REGISTER;
    }

    /**
     * Tạo tài khoản rồi đăng nhập luôn. Lỗi nhập liệu thì hiện lại form với lỗi ở từng ô;
     * thành công thì redirect về trang chủ (PRG).
     */
    @PostMapping(ApiConstants.REGISTER_PATH)
    public String register(@AuthenticationPrincipal AppUserPrincipal principal,
                           @Valid @ModelAttribute(ViewConstants.ATTR_FORM) RegisterForm form,
                           BindingResult bindingResult, HttpServletRequest request, RedirectAttributes redirect) {
        if (principal != null) {
            return ViewConstants.REDIRECT_HOME;
        }
        if (bindingResult.hasErrors()) {
            return ViewConstants.AUTH_REGISTER;
        }
        String username;
        try {
            username = registrationService.register(form);
        } catch (FieldValidationException exception) {
            FormErrors.apply(bindingResult, exception);
            return ViewConstants.AUTH_REGISTER;
        }
        return loginAfterRegistration(request, redirect, username, form);
    }

    /**
     * Đăng nhập qua AuthenticationManager của Spring Security (lưu ngữ cảnh vào phiên, phát sự kiện đăng nhập)
     * thay vì tự dựng Authentication. Tài khoản đã tạo xong, nên nếu bước này trục trặc thì chỉ cần mời
     * người dùng đăng nhập tay.
     */
    private String loginAfterRegistration(HttpServletRequest request, RedirectAttributes redirect,
                                          String username, RegisterForm form) {
        try {
            request.login(username, form.getPassword());
            // Khác với form login, request.login() KHÔNG chạy bước chống session fixation: id phiên trước và sau
            // đăng nhập vẫn là một. Phải tự đổi, nếu không kẻ đã cài sẵn id phiên cho nạn nhân sẽ dùng chung
            // phiên vừa đăng nhập đó.
            request.changeSessionId();
        } catch (ServletException exception) {
            log.warn("Đăng ký xong nhưng không tự đăng nhập được cho {}", username, exception);
            return ViewConstants.REDIRECT_LOGIN;
        }
        flash.success(redirect, MessageKeys.FLASH_REGISTERED, form.getDisplayName().trim());
        return ViewConstants.REDIRECT_HOME;
    }
}
