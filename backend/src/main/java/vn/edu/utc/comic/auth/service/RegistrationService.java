package vn.edu.utc.comic.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.auth.dto.RegisterForm;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.exception.FieldValidationException.FieldViolation;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.repository.UserAccountRepository;
import vn.edu.utc.comic.user.service.PasswordPolicy;

/**
 * Tự đăng ký tài khoản. Tài khoản mới luôn có vai trò USER; quyền tác giả chỉ có qua quy trình duyệt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_CONFIRM_PASSWORD = "confirmPassword";

    private final UserAccountRepository userAccountRepository;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;

    /**
     * Tạo tài khoản độc giả mới.
     *
     * @return tên đăng nhập đã chuẩn hóa (bỏ khoảng trắng thừa), dùng để đăng nhập ngay sau đó
     * @throws FieldValidationException gom MỌI lỗi một lần, gắn theo ô nhập: tên đăng nhập hoặc email đã
     *                                  có người dùng, mật khẩu chưa đạt chính sách, xác nhận không khớp
     */
    @Transactional
    public String register(RegisterForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        validate(form, username, email);

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(form.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        saveGuardingAgainstRace(user);
        log.info("Tài khoản mới {} đã đăng ký", user.getId());
        return username;
    }

    private void validate(RegisterForm form, String username, String email) {
        List<FieldViolation> violations = new ArrayList<>();
        if (userAccountRepository.existsByUsername(username)) {
            violations.add(new FieldViolation(FIELD_USERNAME, MessageKeys.ERROR_USERNAME_DUPLICATED));
        }
        if (userAccountRepository.existsByEmail(email)) {
            violations.add(new FieldViolation(FIELD_EMAIL, MessageKeys.ERROR_EMAIL_DUPLICATED));
        }
        passwordPolicy.check(FIELD_PASSWORD, form.getPassword()).ifPresent(violations::add);
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            violations.add(new FieldViolation(FIELD_CONFIRM_PASSWORD, MessageKeys.ERROR_PASSWORD_CONFIRM_MISMATCH));
        }
        if (!violations.isEmpty()) {
            throw new FieldValidationException(violations);
        }
    }

    /**
     * Hai người đăng ký cùng tên đúng một lúc thì cả hai đều qua được bước kiểm tra ở trên; UNIQUE của cơ sở
     * dữ liệu chặn người đến sau. Đổi lỗi đó thành lỗi nhập liệu thay vì để rơi thành lỗi 500.
     */
    private void saveGuardingAgainstRace(UserAccount user) {
        try {
            userAccountRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Đăng ký trùng tên đăng nhập hoặc email do hai request đồng thời", exception);
            throw new FieldValidationException(
                    List.of(new FieldViolation(FIELD_USERNAME, MessageKeys.ERROR_REGISTRATION_CONFLICT)));
        }
    }
}
