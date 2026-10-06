package vn.edu.utc.comic.user.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.exception.FieldValidationException.FieldViolation;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;

/**
 * Chính sách mật khẩu dùng chung cho đăng ký và đổi mật khẩu: đủ dài (độ dài tối thiểu đọc từ setting)
 * và có cả chữ lẫn số.
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    private final SettingService settingService;

    /**
     * @param field    tên ô nhập mật khẩu trên form, để lỗi hiện đúng chỗ
     * @param password mật khẩu người dùng nhập
     * @return vi phạm gắn với ô nhập nếu mật khẩu chưa đạt; rỗng nếu đạt
     */
    public Optional<FieldViolation> check(String field, String password) {
        int minLength = settingService.getInt(SettingKeys.SECURITY_PASSWORD_MIN_LENGTH,
                SecurityConstants.DEFAULT_PASSWORD_MIN_LENGTH);
        boolean satisfied = password != null
                && password.length() >= minLength
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        return satisfied
                ? Optional.empty()
                : Optional.of(new FieldViolation(field, MessageKeys.ERROR_PASSWORD_WEAK, minLength));
    }
}
