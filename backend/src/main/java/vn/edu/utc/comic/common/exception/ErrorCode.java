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
    STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, MessageKeys.ERROR_STORAGE_FAILED),

    // ----- Tài khoản -----
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, MessageKeys.ERROR_USER_NOT_FOUND),
    USER_SELF_BAN(HttpStatus.CONFLICT, MessageKeys.ERROR_USER_SELF_BAN),

    // ----- Thể loại -----
    GENRE_NOT_FOUND(HttpStatus.NOT_FOUND, MessageKeys.ERROR_GENRE_NOT_FOUND),
    GENRE_IN_USE(HttpStatus.CONFLICT, MessageKeys.ERROR_GENRE_IN_USE),

    // ----- Truyện & chương -----
    // Nội dung không tồn tại và nội dung không được phép xem dùng chung mã 404 để không lộ bản nháp
    STORY_NOT_FOUND(HttpStatus.NOT_FOUND, MessageKeys.ERROR_STORY_NOT_FOUND),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND, MessageKeys.ERROR_CHAPTER_NOT_FOUND),

    // ----- Tương tác của độc giả -----
    RATING_OWN_STORY(HttpStatus.CONFLICT, MessageKeys.ERROR_RATING_OWN_STORY),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, MessageKeys.ERROR_COMMENT_NOT_FOUND),
    COMMENT_TOO_FAST(HttpStatus.TOO_MANY_REQUESTS, MessageKeys.ERROR_COMMENT_TOO_FAST),
    COMMENT_PARENT_INVALID(HttpStatus.BAD_REQUEST, MessageKeys.ERROR_COMMENT_PARENT_INVALID);

    private final HttpStatus httpStatus;
    private final String messageKey;
}
