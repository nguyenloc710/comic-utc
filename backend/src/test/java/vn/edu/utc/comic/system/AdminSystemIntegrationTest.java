package vn.edu.utc.comic.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Tham số vận hành và nhật ký kiểm toán. Bảng setting dùng chung cho mọi test nên luôn trả lại giá trị cũ.
 */
class AdminSystemIntegrationTest extends AbstractIntegrationTest {

    private static final String COOLDOWN_KEY = "comment.cooldown.seconds";
    private static final String CHAT_ENABLED_KEY = "chat.enabled";

    @Autowired
    private SettingService settingService;

    @Test
    void settingsPage_listsEveryParameterByGroup() throws Exception {
        mockMvc.perform(get("/admin/settings").with(user(principalOf(Role.AUTHOR)))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/settings").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_SETTINGS))
                .andExpect(content().string(containsString(COOLDOWN_KEY)))
                .andExpect(content().string(containsString("COMMENT")));
    }

    @Test
    void updatingASetting_takesEffectImmediately_andIsAudited() throws Exception {
        String original = currentValue(COOLDOWN_KEY);
        UserAccount admin = createAccount(Role.ADMIN);
        try {
            mockMvc.perform(post("/admin/settings/{key}", COOLDOWN_KEY).with(csrf()).with(user(principalOf(admin)))
                            .param("value", " 007 "))
                    .andExpect(redirectedUrl("/admin/settings"))
                    .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

            assertThat(currentValue(COOLDOWN_KEY)).isEqualTo("7");
            // Cache đã được xóa: service đọc ngay giá trị mới
            assertThat(settingService.getInt(COOLDOWN_KEY, 0)).isEqualTo(7);
            mockMvc.perform(get("/admin/audit-logs").param("action", "SETTING_CHANGED").param("actor", admin.getUsername())
                            .with(user(principalOf(admin))))
                    .andExpect(status().isOk())
                    .andExpect(view().name(ViewConstants.ADMIN_AUDIT_LOGS))
                    .andExpect(content().string(containsString(COOLDOWN_KEY)));
        } finally {
            restore(COOLDOWN_KEY, original);
        }
    }

    @Test
    void invalidValues_areRefused_andLeaveTheSettingUntouched() throws Exception {
        String originalCooldown = currentValue(COOLDOWN_KEY);
        String originalChat = currentValue(CHAT_ENABLED_KEY);
        try {
            mockMvc.perform(post("/admin/settings/{key}", COOLDOWN_KEY).with(csrf()).with(user(principalOf(Role.ADMIN)))
                            .param("value", "mười lăm"))
                    .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
            mockMvc.perform(post("/admin/settings/{key}", CHAT_ENABLED_KEY).with(csrf()).with(user(principalOf(Role.ADMIN)))
                            .param("value", "yes"))
                    .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
            mockMvc.perform(post("/admin/settings/{key}", CHAT_ENABLED_KEY).with(csrf()).with(user(principalOf(Role.ADMIN)))
                            .param("value", "FALSE"))
                    .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
            mockMvc.perform(post("/admin/settings/{key}", "khong.co.tham.so.nay").with(csrf()).with(user(principalOf(Role.ADMIN)))
                            .param("value", "1"))
                    .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

            assertThat(currentValue(COOLDOWN_KEY)).isEqualTo(originalCooldown);
            assertThat(currentValue(CHAT_ENABLED_KEY)).isEqualTo("false");
        } finally {
            restore(COOLDOWN_KEY, originalCooldown);
            restore(CHAT_ENABLED_KEY, originalChat);
        }
    }

    @Test
    void readOnlySetting_cannotBeChanged() throws Exception {
        String original = currentValue(COOLDOWN_KEY);
        jdbcTemplate.update("UPDATE setting SET is_read_only = 1 WHERE setting_key = ?", COOLDOWN_KEY);
        try {
            mockMvc.perform(post("/admin/settings/{key}", COOLDOWN_KEY).with(csrf()).with(user(principalOf(Role.ADMIN)))
                            .param("value", "99"))
                    .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
            assertThat(currentValue(COOLDOWN_KEY)).isEqualTo(original);
        } finally {
            jdbcTemplate.update("UPDATE setting SET is_read_only = 0 WHERE setting_key = ?", COOLDOWN_KEY);
            restore(COOLDOWN_KEY, original);
        }
    }

    private String currentValue(String key) {
        return jdbcTemplate.queryForObject("SELECT setting_value FROM setting WHERE setting_key = ?", String.class, key);
    }

    private void restore(String key, String value) {
        jdbcTemplate.update("UPDATE setting SET setting_value = ? WHERE setting_key = ?", value, key);
        settingService.clearCache();
    }
}
