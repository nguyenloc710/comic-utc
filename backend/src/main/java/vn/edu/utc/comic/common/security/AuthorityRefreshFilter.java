package vn.edu.utc.comic.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.utc.comic.common.constant.ApiConstants;

/**
 * Giữ cho phiên đang đăng nhập khớp với tài khoản trong cơ sở dữ liệu.
 *
 * <p>Spring Security chỉ nạp tài khoản một lần lúc đăng nhập. Không có filter này thì độc giả vừa được duyệt
 * làm tác giả phải đăng xuất rồi đăng nhập lại mới thấy khu vực tác giả, còn tài khoản vừa bị quản trị viên
 * khóa vẫn dùng tiếp cho tới khi hết phiên. Ở mỗi request, filter so trạng thái trong phiên với trạng thái
 * hiện tại (đọc qua cache):
 * <ul>
 *   <li>tài khoản bị khóa hoặc không còn tồn tại → hủy phiên;</li>
 *   <li>vai trò, tên hiển thị hay ảnh đại diện đã đổi → dựng lại principal và lưu vào phiên.</li>
 * </ul>
 *
 * <p>Lớp này cố ý KHÔNG phải bean: bean kiểu Filter sẽ bị Spring Boot đăng ký thêm lần nữa cho mọi request
 * ở ngoài chuỗi bảo mật. SecurityConfig tự tạo và đặt nó vào đúng vị trí trong chuỗi.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthorityRefreshFilter extends OncePerRequestFilter {

    private final AccountStateService accountStateService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return ApiConstants.STATIC_RESOURCE_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Optional<AppUserPrincipal> principal = SecurityUtils.getCurrentPrincipal();
        if (principal.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        Optional<AccountState> currentState = accountStateService.findState(principal.get().getId())
                .filter(AccountState::isActive);
        if (currentState.isEmpty()) {
            rejectBannedAccount(request, response, filterChain, principal.get());
            return;
        }
        if (!currentState.get().equals(principal.get().getAccountState())) {
            refreshSession(request, response, principal.get().withState(currentState.get()));
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Hủy phiên của tài khoản đã bị khóa. Trang thường được đưa về trang đăng nhập kèm lý do; lời gọi API đi
     * tiếp với tư cách khách để nhận JSON 401 như mọi trường hợp hết phiên khác.
     */
    private void rejectBannedAccount(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain, AppUserPrincipal principal)
            throws ServletException, IOException {
        log.info("Hủy phiên của tài khoản {} vì đã bị khóa hoặc không còn tồn tại", principal.getId());
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        if (request.getRequestURI().startsWith(ApiConstants.API_ROOT + "/")) {
            filterChain.doFilter(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + LoginError.BANNED.toLoginPath());
    }

    private void refreshSession(HttpServletRequest request, HttpServletResponse response,
                                AppUserPrincipal refreshedPrincipal) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        UsernamePasswordAuthenticationToken refreshed = UsernamePasswordAuthenticationToken.authenticated(
                refreshedPrincipal, current.getCredentials(), refreshedPrincipal.getAuthorities());
        refreshed.setDetails(current.getDetails());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(refreshed);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        log.debug("Đã làm mới phiên của tài khoản {}", refreshedPrincipal.getId());
    }
}
