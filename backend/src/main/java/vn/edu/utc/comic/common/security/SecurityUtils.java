package vn.edu.utc.comic.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tiện ích đọc thông tin tài khoản đang đăng nhập từ ngữ cảnh bảo mật.
 */
public final class SecurityUtils {

    /** Id tài khoản đang thao tác; null khi là khách vãng lai hoặc chạy trong job nền. */
    public static Long getCurrentUserId() {
        return getCurrentPrincipal().map(AppUserPrincipal::getId).orElse(null);
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentPrincipal().map(AppUserPrincipal::getUsername);
    }

    public static Optional<AppUserPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AppUserPrincipal principal
                ? Optional.of(principal)
                : Optional.empty();
    }

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
