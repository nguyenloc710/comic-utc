package vn.edu.utc.comic.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;

/**
 * Trang đăng nhập (form do Spring Security xử lý). Đăng ký, hồ sơ và đổi mật khẩu được bổ sung ở giai đoạn 2.
 */
@Controller
public class AuthController {

    /** Trang đăng nhập; đã đăng nhập thì về trang chủ thay vì hiện lại form. */
    @GetMapping(ApiConstants.LOGIN_PATH)
    public String showLogin(@AuthenticationPrincipal AppUserPrincipal principal) {
        return principal == null ? ViewConstants.AUTH_LOGIN : ViewConstants.REDIRECT_HOME;
    }
}
