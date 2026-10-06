package vn.edu.utc.comic.common.config;

import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.security.AccountStateService;
import vn.edu.utc.comic.common.security.ApiErrorResponder;
import vn.edu.utc.comic.common.security.AuthorityRefreshFilter;
import vn.edu.utc.comic.common.security.LoginFailureHandler;

/**
 * Cấu hình bảo mật: form login + session + CSRF; phân quyền theo khu vực URL.
 *
 * <p>Khác các hệ thống nội bộ, đây là website công khai: khách vãng lai đọc được truyện. Vì vậy mặc định
 * là cho qua, còn mọi thứ cần đăng nhập PHẢI nằm dưới một trong các tiền tố /me, /studio, /admin, /api
 * (controller ở các khu vực đó có thêm @PreAuthorize làm lớp thứ hai).
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ALL_SUB_PATHS = "/**";
    private static final String[] ADMIN_ONLY_TOOL_PATHS = {
            "/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
    };

    private final ApiErrorResponder apiErrorResponder;
    private final LoginFailureHandler loginFailureHandler;
    private final AccountStateService accountStateService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher apiRequests = PathPatternRequestMatcher.withDefaults()
                .matcher(ApiConstants.API_ROOT + ALL_SUB_PATHS);
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiConstants.HEALTH_PATH).permitAll()
                        .requestMatchers(ADMIN_ONLY_TOOL_PATHS).hasRole(SecurityConstants.ROLE_ADMIN)
                        .requestMatchers(ApiConstants.ADMIN_ROOT + ALL_SUB_PATHS).hasRole(SecurityConstants.ROLE_ADMIN)
                        .requestMatchers(ApiConstants.STUDIO_ROOT + ALL_SUB_PATHS,
                                ApiConstants.API_STUDIO_PATH + ALL_SUB_PATHS).hasRole(SecurityConstants.ROLE_AUTHOR)
                        .requestMatchers(ApiConstants.ME_ROOT + ALL_SUB_PATHS).hasRole(SecurityConstants.ROLE_USER)
                        .requestMatchers(ApiConstants.API_ROOT + ALL_SUB_PATHS).authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage(ApiConstants.LOGIN_PATH)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                // Đặt ngay trước bước kiểm tra quyền: lúc này phiên đã được nạp, và quyền vừa làm mới
                // được dùng cho chính request đang xử lý
                .addFilterBefore(new AuthorityRefreshFilter(accountStateService), AuthorizationFilter.class)
                .logout(logout -> logout
                        .logoutSuccessUrl(ApiConstants.HOME_PATH)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session.sessionFixation().migrateSession())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint(apiRequests))
                        .defaultAccessDeniedHandlerFor(apiErrorResponder, apiRequests)
                        .accessDeniedPage(ApiConstants.FORBIDDEN_PAGE_PATH))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(policy -> policy
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(SecurityConstants.CONTENT_SECURITY_POLICY)))
                .build();
    }

    /**
     * Khách mở trang cần đăng nhập thì chuyển hướng sang trang đăng nhập; riêng /api/** trả JSON 401 để lời gọi
     * fetch (khung chat, theo dõi, bình luận) biết là hết phiên chứ không nhận về trang HTML.
     *
     * <p>Tự khai báo cả hai nhánh thay vì dùng defaultAuthenticationEntryPointFor: với cách đó nhánh "còn lại"
     * do Spring chọn theo header Accept, nên request không gửi Accept: text/html lại nhận JSON của nhánh API.
     */
    private AuthenticationEntryPoint authenticationEntryPoint(RequestMatcher apiRequests) {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(apiRequests, apiErrorResponder);
        DelegatingAuthenticationEntryPoint delegating = new DelegatingAuthenticationEntryPoint(entryPoints);
        delegating.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint(ApiConstants.LOGIN_PATH));
        return delegating;
    }

    /**
     * Tác giả và quản trị viên đều làm được mọi việc của độc giả. Quản trị viên KHÔNG tự động là tác giả:
     * không có bút danh nên không vào khu vực /studio.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(SecurityConstants.ROLE_ADMIN).implies(SecurityConstants.ROLE_USER)
                .role(SecurityConstants.ROLE_AUTHOR).implies(SecurityConstants.ROLE_USER)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(SecurityConstants.BCRYPT_STRENGTH);
    }
}
