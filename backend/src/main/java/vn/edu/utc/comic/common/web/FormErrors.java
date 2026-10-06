package vn.edu.utc.comic.common.web;

import org.springframework.validation.BindingResult;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.FieldValidationException;

/**
 * Đưa lỗi nghiệp vụ do service ném ra vào đúng ô nhập của form Thymeleaf.
 *
 * <p>Nhờ vậy lỗi như "tên đăng nhập đã có người dùng" hiện ngay dưới ô tương ứng và form giữ nguyên những gì
 * người dùng đã gõ, thay vì redirect kèm một thông báo chung rồi bắt nhập lại từ đầu. Khóa thông báo được
 * truyền làm mã lỗi nên th:errors tự dịch qua messages.properties.
 */
public final class FormErrors {

    /** Gắn từng vi phạm vào ô nhập cùng tên. */
    public static void apply(BindingResult bindingResult, FieldValidationException exception) {
        exception.getViolations().forEach(violation -> bindingResult.rejectValue(
                violation.field(), violation.messageKey(), violation.arguments(), violation.messageKey()));
    }

    /** Gắn một lỗi nghiệp vụ (chỉ có thông báo chung) vào ô nhập đã gây ra nó, ví dụ ô chọn ảnh. */
    public static void apply(BindingResult bindingResult, String field, ApiException exception) {
        String messageKey = exception.getErrorCode().getMessageKey();
        bindingResult.rejectValue(field, messageKey, exception.getArguments(), messageKey);
    }

    private FormErrors() {
        throw new UnsupportedOperationException("Utility class");
    }
}
