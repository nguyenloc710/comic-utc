package vn.edu.utc.comic.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.i18n.MessageService;

/**
 * Trả lỗi bảo mật dưới dạng JSON cho nhóm {@code /api/**}.
 *
 * <p>Mặc định của Spring Security là chuyển hướng 302 sang trang đăng nhập. Với trình duyệt thì
 * đúng, nhưng với lời gọi {@code fetch} của khung chat hay nút theo dõi thì sai: hết phiên là nó lặng lẽ
 * đi theo chuyển hướng, nhận về trang HTML đăng nhập rồi báo một lỗi khó hiểu. Trả 401 kèm mã lỗi
 * giúp giao diện biết chính xác chuyện gì xảy ra.
 *
 * <p>Phần còn lại của ứng dụng (các trang Thymeleaf) vẫn giữ nguyên hành vi chuyển hướng.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode.name(),
                messageService.getMessage(errorCode.getMessageKey()), null));
    }
}
