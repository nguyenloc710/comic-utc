package vn.edu.utc.comic.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.constant.UserConstants;

/** Form đổi mật khẩu của người đang đăng nhập. */
@Getter
@Setter
public class ChangePasswordForm {

    @NotBlank(message = "{validation.password.current.required}")
    private String currentPassword;

    @NotBlank(message = "{validation.password.required}")
    @Size(max = UserConstants.PASSWORD_MAX_LENGTH, message = "{validation.password.size}")
    private String newPassword;

    @NotBlank(message = "{validation.password.confirm.required}")
    private String confirmPassword;
}
