package vn.edu.utc.comic.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.security.SecurityUtils;

/**
 * Gắn requestId và username vào MDC để mọi dòng log của một request dò chéo được với audit_log,
 * đồng thời ghi access log (method, URI, status, thời gian xử lý).
 */
@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USERNAME = "username";
    private static final int REQUEST_ID_LENGTH = 8;
    private static final String ANONYMOUS = "-";

    private static final String ACTUATOR_PREFIX = "/actuator";

    /** Tài nguyên tĩnh, ảnh truyện và lời gọi kiểm tra sức khỏe định kỳ: ghi log sẽ chỉ gây nhiễu. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith(ACTUATOR_PREFIX)
                || ApiConstants.STATIC_RESOURCE_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH);
        MDC.put(MDC_REQUEST_ID, requestId);
        request.setAttribute(MDC_REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Username chỉ có sau khi filter bảo mật chạy, nên đặt lúc ghi access log
            MDC.put(MDC_USERNAME, SecurityUtils.getCurrentUsername().orElse(ANONYMOUS));
            log.info("ACCESS {} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), System.currentTimeMillis() - start);
            MDC.clear();
        }
    }
}
