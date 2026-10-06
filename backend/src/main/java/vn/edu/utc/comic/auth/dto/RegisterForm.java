package vn.edu.utc.comic.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.constant.UserConstants;

/**
 * Form tự đăng ký tài khoản độc giả. Ở đây chỉ kiểm tra hình dạng dữ liệu; trùng tên đăng nhập / email
 * và độ mạnh mật khẩu do RegistrationService kiểm tra.
 */
@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "{validation.username.required}")
    @Pattern(regexp = UserConstants.USERNAME_PATTERN, message = "{validation.username.pattern}")
    private String username;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = UserConstants.EMAIL_MAX_LENGTH, message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.display.name.required}")
    @Size(max = UserConstants.DISPLAY_NAME_MAX_LENGTH, message = "{validation.display.name.size}")
    private String displayName;

    @NotBlank(message = "{validation.password.required}")
    @Size(max = UserConstants.PASSWORD_MAX_LENGTH, message = "{validation.password.size}")
    private String password;

    @NotBlank(message = "{validation.password.confirm.required}")
    private String confirmPassword;
}
