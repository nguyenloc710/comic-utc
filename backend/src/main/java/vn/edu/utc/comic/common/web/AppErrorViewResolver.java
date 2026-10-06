package vn.edu.utc.comic.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.i18n.MessageService;

/**
 * Lỗi xảy ra ngoài tầm {@code GlobalExceptionHandler} (ví dụ hỏng khi đang render view, lỗi filter)
 * được servlet container chuyển tới /error; chỗ này đưa chúng về cùng trang lỗi của ứng dụng
 * thay vì trang Whitelabel mặc định.
 */
@Component
@RequiredArgsConstructor
public class AppErrorViewResolver implements ErrorViewResolver {

    private final MessageService messageService;

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        ModelAndView modelAndView = new ModelAndView(ViewConstants.ERROR_PAGE);
        modelAndView.setStatus(status);
        modelAndView.addObject(ViewConstants.ATTR_ERROR_STATUS, status.value());
        modelAndView.addObject(ViewConstants.ATTR_ERROR_MESSAGE,
                messageService.getMessage(toErrorCode(status).getMessageKey()));
        return modelAndView;
    }

    private static ErrorCode toErrorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
