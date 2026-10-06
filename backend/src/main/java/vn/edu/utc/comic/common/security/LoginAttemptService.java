package vn.edu.utc.comic.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Đếm số lần đăng nhập sai và khóa tạm tài khoản.
 *
 * <p>Ngưỡng và thời gian khóa đọc từ bảng setting để quản trị viên đổi được mà không cần build lại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserAccountRepository userAccountRepository;
    private final SettingService settingService;
    private final Clock clock;

    /**
     * Ghi nhận một lần sai mật khẩu; chạm ngưỡng thì khóa tạm. Tên đăng nhập lạ thì bỏ qua.
     *
     * <p>Cộng và khóa bằng hai câu UPDATE có điều kiện thay vì đọc-rồi-ghi: kẻ dò mật khẩu gửi nhiều request
     * song song, nếu mỗi request tự đọc số lần sai rồi ghi lại thì chúng cùng thấy 0 và vượt qua ngưỡng.
     */
    @Transactional
    public void recordFailure(String usernameOrEmail) {
        userAccountRepository.findIdByUsernameOrEmail(usernameOrEmail).ifPresent(this::increaseFailure);
    }

    /** Đăng nhập thành công thì xóa đếm sai, gỡ khóa tạm và ghi thời điểm đăng nhập. */
    @Transactional
    public void recordSuccess(Long userId) {
        userAccountRepository.resetFailedAttempts(userId, clock.instant());
    }

    private void increaseFailure(Long userId) {
        int maxAttempts = settingService.getInt(SettingKeys.SECURITY_LOGIN_MAX_FAILED_ATTEMPTS,
                SecurityConstants.DEFAULT_MAX_FAILED_ATTEMPTS);
        int lockMinutes = settingService.getInt(SettingKeys.SECURITY_LOGIN_LOCK_MINUTES,
                SecurityConstants.DEFAULT_LOCK_MINUTES);
        Instant lockedUntil = clock.instant().plus(Duration.ofMinutes(lockMinutes));

        userAccountRepository.incrementFailedAttempts(userId);
        if (userAccountRepository.lockIfAttemptsReached(userId, maxAttempts, lockedUntil) > 0) {
            log.warn("Khóa tạm tài khoản {} tới {} sau {} lần sai mật khẩu", userId, lockedUntil, maxAttempts);
        }
    }
}
