package vn.edu.utc.comic.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.edu.utc.comic.common.logging.RequestLoggingFilter;

/**
 * Ghi nhật ký kiểm toán cho thao tác nhạy cảm.
 *
 * <p>Ghi bằng transaction riêng (REQUIRES_NEW) để dòng audit vẫn còn khi nghiệp vụ rollback,
 * và ngược lại lỗi ghi audit không làm hỏng nghiệp vụ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int USER_AGENT_MAX_LENGTH = 255;

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Ghi một thao tác không gắn với bản ghi cụ thể nào, với người thực hiện chỉ định
     * (dùng cho sự kiện đăng nhập, khi ngữ cảnh bảo mật chưa có hoặc không còn principal).
     *
     * @param actorId   id tài khoản; {@code null} khi đăng nhập thất bại
     * @param actorName tên đăng nhập người dùng đã nhập
     * @param detail    thông tin thêm, được lưu dạng JSON; có thể {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, Long actorId, String actorName, Object detail) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setActorId(actorId);
            entry.setActorName(actorName);
            entry.setDetail(toJson(detail));
            entry.setCreatedAt(clock.instant());
            fillRequestInfo(entry);
            auditLogRepository.save(entry);
        } catch (RuntimeException exception) {
            log.error("Không ghi được audit {} cho {}", action, actorName, exception);
        }
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            log.warn("Không chuyển được chi tiết audit sang JSON", exception);
            return null;
        }
    }

    private static void fillRequestInfo(AuditLog entry) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        entry.setIpAddress(request.getRemoteAddr());
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            entry.setUserAgent(userAgent.substring(0, Math.min(USER_AGENT_MAX_LENGTH, userAgent.length())));
        }
        Object requestId = request.getAttribute(RequestLoggingFilter.MDC_REQUEST_ID);
        entry.setRequestId(requestId == null ? null : requestId.toString());
    }
}
