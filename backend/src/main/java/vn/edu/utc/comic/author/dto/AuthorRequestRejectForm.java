package vn.edu.utc.comic.author.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.constant.AuthorConstants;

/** Form quản trị viên từ chối một yêu cầu đăng ký tác giả. */
@Getter
@Setter
public class AuthorRequestRejectForm {

    /** Bắt buộc: người gửi đọc được lý do này và dựa vào đó để sửa trước khi gửi lại. */
    @NotBlank(message = "{validation.author.reject.reason.required}")
    @Size(max = AuthorConstants.REJECT_REASON_MAX_LENGTH, message = "{validation.author.reject.reason.size}")
    private String rejectReason;
}
