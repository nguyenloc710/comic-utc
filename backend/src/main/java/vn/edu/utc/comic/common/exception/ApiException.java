package vn.edu.utc.comic.common.exception;

import java.io.Serial;
import lombok.Getter;

/**
 * Ngoại lệ nghiệp vụ của hệ thống.
 *
 * <p>Chỉ mang mã lỗi và tham số điền vào thông báo — KHÔNG chứa chuỗi hiển thị.
 */
@Getter
public class ApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient ErrorCode errorCode;

    /** Tham số điền vào chỗ {0}, {1}... của thông báo. */
    private final transient Object[] arguments;

    public ApiException(ErrorCode errorCode, Object... arguments) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.arguments = arguments;
    }

    public ApiException(ErrorCode errorCode, Throwable cause, Object... arguments) {
        super(errorCode.name(), cause);
        this.errorCode = errorCode;
        this.arguments = arguments;
    }
}
