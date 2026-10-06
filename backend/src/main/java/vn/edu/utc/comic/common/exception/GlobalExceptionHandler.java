package vn.edu.utc.comic.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.dto.ApiResponse.FieldErrorItem;
import vn.edu.utc.comic.common.i18n.MessageService;

/**
 * Nơi DUY NHẤT chuyển ngoại lệ thành phản hồi lỗi và dịch thông báo.
 *
 * <p>Request tới {@code /api/**} (hoặc chấp nhận JSON) nhận {@link ApiResponse}; request trang thường
 * nhận trang lỗi Thymeleaf. Lỗi nghiệp vụ trong form POST được controller bắt và đưa vào flash
 * trước khi tới đây, nên chỗ này chỉ còn xử lý các trường hợp bất ngờ.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    /** Lỗi nghiệp vụ do hệ thống chủ động ném ra. */
    @ExceptionHandler(ApiException.class)
    public Object handleApiException(ApiException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        String message = messageService.getMessage(errorCode.getMessageKey(), exception.getArguments());

        // Lỗi 5xx mới cần stacktrace; lỗi 4xx là tình huống nghiệp vụ bình thường
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("Lỗi nghiệp vụ {} tại {}", errorCode, request.getRequestURI(), exception);
        } else {
            log.debug("Lỗi nghiệp vụ {} tại {}", errorCode, request.getRequestURI());
        }
        return buildResponse(request, errorCode, message, null);
    }

    /** Lỗi nghiệp vụ gắn theo ô nhập: dịch từng vi phạm rồi trả cùng cấu trúc với Bean Validation. */
    @ExceptionHandler(FieldValidationException.class)
    public Object handleFieldValidation(FieldValidationException exception, HttpServletRequest request) {
        List<FieldErrorItem> fieldErrors = exception.getViolations().stream()
                .map(violation -> new FieldErrorItem(violation.field(),
                        messageService.getMessage(violation.messageKey(), violation.arguments())))
                .toList();
        return buildResponse(request, ErrorCode.VALIDATION_ERROR,
                messageService.getMessage(ErrorCode.VALIDATION_ERROR.getMessageKey()), fieldErrors);
    }

    /** Dữ liệu JSON không hợp lệ theo Bean Validation (form MVC xử lý qua BindingResult, không tới đây). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldErrorItem> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorItem)
                .toList();
        return buildResponse(request, ErrorCode.VALIDATION_ERROR,
                messageService.getMessage(ErrorCode.VALIDATION_ERROR.getMessageKey()), fieldErrors);
    }

    /** Tệp tải lên vượt giới hạn của servlet container (trước cả khi tới service). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleUploadTooLarge(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        String message = messageService.getMessage(ErrorCode.UPLOAD_TOO_LARGE.getMessageKey());
        return buildResponse(request, ErrorCode.UPLOAD_TOO_LARGE, message, null);
    }

    /** Đã đăng nhập nhưng không đủ quyền (bị @PreAuthorize chặn). */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        log.debug("Từ chối truy cập do thiếu quyền tại {}", request.getRequestURI());
        String message = messageService.getMessage(ErrorCode.FORBIDDEN.getMessageKey());
        return buildResponse(request, ErrorCode.FORBIDDEN, message, null);
    }

    /** Gọi vào đường dẫn không tồn tại. */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(NoResourceFoundException exception, HttpServletRequest request) {
        String message = messageService.getMessage(ErrorCode.RESOURCE_NOT_FOUND.getMessageKey());
        return buildResponse(request, ErrorCode.RESOURCE_NOT_FOUND, message, null);
    }

    /** Lưới an toàn cuối cùng: mọi lỗi ngoài dự kiến. */
    @ExceptionHandler(Exception.class)
    public Object handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Lỗi không lường trước tại {}", request.getRequestURI(), exception);
        String message = messageService.getMessage(ErrorCode.INTERNAL_ERROR.getMessageKey());
        return buildResponse(request, ErrorCode.INTERNAL_ERROR, message, null);
    }

    private FieldErrorItem toFieldErrorItem(FieldError fieldError) {
        return new FieldErrorItem(fieldError.getField(), fieldError.getDefaultMessage());
    }

    /**
     * Request từ fetch() hoặc dưới /api trả JSON; request trang thường trả template lỗi.
     */
    private Object buildResponse(HttpServletRequest request, ErrorCode errorCode,
                                 String message, List<FieldErrorItem> fieldErrors) {
        HttpStatus status = errorCode.getHttpStatus();
        if (isJsonRequest(request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error(errorCode.name(), message, fieldErrors));
        }
        ModelAndView modelAndView = new ModelAndView(ViewConstants.ERROR_PAGE);
        modelAndView.setStatus(status);
        modelAndView.addObject(ViewConstants.ATTR_ERROR_MESSAGE, message);
        modelAndView.addObject(ViewConstants.ATTR_ERROR_STATUS, status.value());
        return modelAndView;
    }

    private static boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        boolean acceptsJson = accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);
        return request.getRequestURI().startsWith(ApiConstants.API_ROOT + "/") || acceptsJson;
    }
}
