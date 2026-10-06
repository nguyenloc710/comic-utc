package vn.edu.utc.comic.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Đăng nhập thất bại thì quay lại trang đăng nhập kèm mã lỗi, để người dùng biết là sai mật khẩu,
 * đang bị khóa tạm hay đã bị quản trị viên khóa.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        redirectStrategy.sendRedirect(request, response, LoginError.fromException(exception).toLoginPath());
    }
}
