package vn.edu.utc.comic.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyTest {

    private static final int MIN_LENGTH = 8;
    private static final String FIELD = "password";

    @Mock
    private SettingService settingService;

    @InjectMocks
    private PasswordPolicy passwordPolicy;

    @BeforeEach
    void setUp() {
        // lenient: một test đặt lại độ dài tối thiểu khác, khi đó stub mặc định này không được dùng tới
        lenient().when(settingService.getInt(eq(SettingKeys.SECURITY_PASSWORD_MIN_LENGTH), anyInt()))
                .thenReturn(MIN_LENGTH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"matkhau1", "Mật khẩu 2026", "12345678a"})
    void check_acceptsPasswordLongEnoughWithLetterAndDigit(String password) {
        assertThat(passwordPolicy.check(FIELD, password)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "ngan1", "chitoanchucai", "1234567890"})
    void check_rejectsShortOrSingleClassPassword(String password) {
        assertThat(passwordPolicy.check(FIELD, password)).hasValueSatisfying(violation -> {
            assertThat(violation.field()).isEqualTo(FIELD);
            assertThat(violation.messageKey()).isEqualTo(MessageKeys.ERROR_PASSWORD_WEAK);
            assertThat(violation.arguments()).containsExactly(MIN_LENGTH);
        });
    }

    @Test
    void check_followsMinLengthFromSetting() {
        when(settingService.getInt(eq(SettingKeys.SECURITY_PASSWORD_MIN_LENGTH), anyInt())).thenReturn(12);

        assertThat(passwordPolicy.check(FIELD, "matkhau1")).isPresent();
        assertThat(passwordPolicy.check(FIELD, "matkhaudai12")).isEmpty();
    }
}
