package vn.edu.utc.comic.common.exception;

import java.io.Serial;
import java.util.List;
import lombok.Getter;

/**
 * Lỗi kiểm tra nghiệp vụ gắn được vào từng ô nhập (khác {@link ApiException} chỉ có một thông báo chung).
 * Mỗi vi phạm chỉ mang khóa thông báo + tham số; GlobalExceptionHandler mới dịch.
 */
@Getter
public class FieldValidationException extends ApiException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<FieldViolation> violations;

    public FieldValidationException(List<FieldViolation> violations) {
        super(ErrorCode.VALIDATION_ERROR);
        this.violations = List.copyOf(violations);
    }

    /**
     * @param field      tên trường trong DTO
     * @param messageKey khóa trong messages.properties
     * @param arguments  tham số điền vào {0}, {1}…
     */
    public record FieldViolation(String field, String messageKey, Object... arguments) {
    }
}
