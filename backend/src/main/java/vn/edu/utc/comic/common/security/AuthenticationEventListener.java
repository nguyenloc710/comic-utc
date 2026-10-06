package vn.edu.utc.comic.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;

/**
 * Nghe sự kiện xác thực của Spring Security để ghi nhật ký kiểm toán đăng nhập / đăng xuất.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final AuditService auditService;

    @EventListener
    public void handleSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AppUserPrincipal principal) {
            auditService.record(AuditAction.LOGIN_SUCCESS, principal.getId(), principal.getUsername(), null);
        }
    }

    @EventListener
    public void handleFailure(AbstractAuthenticationFailureEvent event) {
        String username = String.valueOf(event.getAuthentication().getPrincipal());
        auditService.record(AuditAction.LOGIN_FAILED, null, username,
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void handleLogout(LogoutSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AppUserPrincipal principal) {
            auditService.record(AuditAction.LOGOUT, principal.getId(), principal.getUsername(), null);
        }
    }
}
