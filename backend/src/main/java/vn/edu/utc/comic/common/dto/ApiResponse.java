package vn.edu.utc.comic.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Khuôn dạng chung của mọi phản hồi JSON dưới /api/**.
 *
 * @param success   true nếu xử lý thành công
 * @param data      dữ liệu trả về khi thành công
 * @param errorCode mã lỗi nghiệp vụ khi thất bại
 * @param message   thông báo đã dịch
 * @param errors    danh sách lỗi theo từng trường khi dữ liệu vào không hợp lệ
 * @param timestamp thời điểm tạo phản hồi
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Khuôn dạng chung của mọi phản hồi API")
public record ApiResponse<T>(
        boolean success,
        T data,
        String errorCode,
        String message,
        List<FieldErrorItem> errors,
        Instant timestamp) {

    /** Một lỗi gắn với một trường dữ liệu đầu vào. */
    public record FieldErrorItem(String field, String message) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, List<FieldErrorItem> errors) {
        return new ApiResponse<>(false, null, errorCode, message, errors, Instant.now());
    }
}
