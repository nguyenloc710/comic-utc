package vn.edu.utc.comic.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

class LoginLockoutIntegrationTest extends AbstractIntegrationTest {

    private static final String WRONG_PASSWORD = "sai-mat-khau";

    @Autowired
    private SettingService settingService;

    @Test
    void account_isTemporarilyLocked_afterTooManyWrongPasswords() throws Exception {
        UserAccount account = createAccount(Role.USER);
        int maxAttempts = settingService.getInt(SettingKeys.SECURITY_LOGIN_MAX_FAILED_ATTEMPTS,
                SecurityConstants.DEFAULT_MAX_FAILED_ATTEMPTS);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            attemptLogin(account, WRONG_PASSWORD).andExpect(redirectedUrl("/login?error=bad_credentials"));
        }

        // Đang bị khóa thì mật khẩu đúng cũng không vào được
        attemptLogin(account, ACCOUNT_PASSWORD)
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=locked"));
        UserAccount locked = reload(account);
        assertThat(locked.getLockedUntil()).isAfter(Instant.now());
        assertThat(locked.getFailedAttempts()).isZero();
    }

    @Test
    void attemptsWhileLocked_doNotExtendTheLock() throws Exception {
        UserAccount account = createAccount(Role.USER);
        // Cắt về micro giây: DATETIME(6) làm tròn phần nano, so sánh với giá trị gốc sẽ lệch ngẫu nhiên
        Instant lockedUntil = Instant.now().plusSeconds(600).truncatedTo(ChronoUnit.MICROS);
        account.setLockedUntil(lockedUntil);
        userAccountRepository.saveAndFlush(account);

        attemptLogin(account, WRONG_PASSWORD).andExpect(redirectedUrl("/login?error=locked"));

        UserAccount afterAttempt = reload(account);
        assertThat(afterAttempt.getFailedAttempts()).isZero();
        assertThat(afterAttempt.getLockedUntil()).isEqualTo(lockedUntil);
    }

    @Test
    void successfulLogin_resetsFailureCounter_andRecordsLoginTime() throws Exception {
        UserAccount account = createAccount(Role.USER);
        attemptLogin(account, WRONG_PASSWORD);
        attemptLogin(account, WRONG_PASSWORD);
        assertThat(reload(account).getFailedAttempts()).isEqualTo(2);

        attemptLogin(account, ACCOUNT_PASSWORD).andExpect(authenticated().withUsername(account.getUsername()));

        UserAccount afterLogin = reload(account);
        assertThat(afterLogin.getFailedAttempts()).isZero();
        assertThat(afterLogin.getLastLoginAt()).isNotNull();
    }

    @Test
    void login_acceptsEmailInPlaceOfUsername() throws Exception {
        UserAccount account = createAccount(Role.USER);

        mockMvc.perform(formLogin("/login").user(account.getEmail()).password(ACCOUNT_PASSWORD))
                .andExpect(authenticated().withUsername(account.getUsername()));
    }

    @Test
    void bannedAccount_cannotLogIn_evenWithCorrectPassword() throws Exception {
        UserAccount account = createAccount(Role.USER);
        account.setStatus(UserStatus.BANNED);
        userAccountRepository.saveAndFlush(account);

        attemptLogin(account, ACCOUNT_PASSWORD)
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=banned"));
    }

    @Test
    void loginPage_explainsEachFailureReason_andTreatsUnknownCodeAsBadCredentials() throws Exception {
        mockMvc.perform(get("/login").param("error", "locked"))
                .andExpect(content().string(Matchers.containsString("khóa tạm")));
        mockMvc.perform(get("/login").param("error", "banned"))
                .andExpect(content().string(Matchers.containsString("quản trị viên khóa")));
        mockMvc.perform(get("/login").param("error", "error.internal"))
                .andExpect(content().string(Matchers.containsString("mật khẩu không đúng")));
    }

    private ResultActions attemptLogin(UserAccount account, String password) throws Exception {
        return mockMvc.perform(formLogin("/login").user(account.getUsername()).password(password));
    }
}
