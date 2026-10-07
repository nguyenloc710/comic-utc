package vn.edu.utc.comic.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.constant.ReportConstants;

/**
 * Form một ô "lý do" của quản trị viên: ẩn truyện / chương / bình luận, xử lý báo cáo.
 * Lý do bắt buộc vì nó được gửi tới tác giả (thông báo) và lưu vào nhật ký kiểm toán.
 */
@Getter
@Setter
public class ReasonForm {

    @NotBlank(message = "{validation.reason.required}")
    @Size(max = ReportConstants.COMMENT_HIDDEN_REASON_MAX_LENGTH, message = "{validation.reason.size}")
    private String reason;
}
