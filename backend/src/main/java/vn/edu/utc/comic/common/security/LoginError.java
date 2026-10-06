package vn.edu.utc.comic.common.security;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;

/**
 * Lý do không đăng nhập được. Mã đi trên URL ({@code /login?error=<code>}), trang đăng nhập đổi mã thành
 * thông báo — không bao giờ lấy thẳng giá trị trên URL làm khóa thông báo.
 */
@Getter
@RequiredArgsConstructor
public enum LoginError {

    BAD_CREDENTIALS("bad_credentials", MessageKeys.LOGIN_ERROR_BAD_CREDENTIALS),
    /** Khóa tạm do nhập sai mật khẩu nhiều lần. */
    LOCKED("locked", MessageKeys.LOGIN_ERROR_LOCKED),
    /** Bị quản trị viên khóa. */
    BANNED("banned", MessageKeys.LOGIN_ERROR_BANNED);

    private final String code;
    private final String messageKey;

    public static Optional<LoginError> fromCode(String code) {
        return Arrays.stream(values()).filter(error -> error.code.equals(code)).findFirst();
    }

    public static LoginError fromException(AuthenticationException exception) {
        if (exception instanceof LockedException) {
            return LOCKED;
        }
        if (exception instanceof DisabledException) {
            return BANNED;
        }
        return BAD_CREDENTIALS;
    }

    /** Đường dẫn trang đăng nhập kèm mã lỗi này. */
    public String toLoginPath() {
        return ApiConstants.LOGIN_PATH + "?" + SecurityConstants.LOGIN_ERROR_PARAM + "=" + code;
    }
}
