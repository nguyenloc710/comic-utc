package vn.edu.utc.comic.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import vn.edu.utc.comic.common.constant.MessageKeys;

/**
 * Mã lỗi nghiệp vụ. Mỗi mã gắn một HTTP status và một khóa thông báo;
 * service chỉ ném mã lỗi, việc dịch do {@link GlobalExceptionHandler} đảm nhiệm.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ----- Chung -----
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, MessageKeys.ERROR_VALIDATION),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, MessageKeys.ERROR_RESOURCE_NOT_FOUND),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, MessageKeys.ERROR_INTERNAL),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, MessageKeys.ERROR_UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN, MessageKeys.ERROR_FORBIDDEN),

    // ----- Ảnh & lưu trữ -----
    IMAGE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, MessageKeys.ERROR_IMAGE_TYPE_NOT_ALLOWED),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, MessageKeys.ERROR_IMAGE_TOO_LARGE),
    UPLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, MessageKeys.ERROR_UPLOAD_TOO_LARGE),
    STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, MessageKeys.ERROR_STORAGE_FAILED);

    private final HttpStatus httpStatus;
    private final String messageKey;
}
