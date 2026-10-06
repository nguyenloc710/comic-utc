package vn.edu.utc.comic.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

class AdminUserIntegrationTest extends AbstractIntegrationTest {

    @Test
    void userList_isForAdminsOnly() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(principalOf(Role.AUTHOR))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/ban").with(user(principalOf(Role.USER))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userList_filtersByKeywordRoleAndStatus() throws Exception {
        UserAccount author = createAccount(Role.AUTHOR);
        UserAccount reader = createAccount(Role.USER);
        AppUserPrincipal admin = principalOf(Role.ADMIN);

        mockMvc.perform(get("/admin/users").param("keyword", author.getUsername().toUpperCase()).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_USERS))
                .andExpect(content().string(containsString(author.getEmail())))
                .andExpect(content().string(not(containsString(reader.getEmail()))));

        // Cùng một từ khóa nhưng lọc sai vai trò hoặc sai trạng thái thì không còn dòng nào
        mockMvc.perform(get("/admin/users").param("keyword", author.getUsername()).param("role", "USER")
                        .with(user(admin)))
                .andExpect(content().string(not(containsString(author.getEmail()))));
        mockMvc.perform(get("/admin/users").param("keyword", author.getUsername()).param("status", "BANNED")
                        .with(user(admin)))
                .andExpect(content().string(not(containsString(author.getEmail()))));
    }

    @Test
    void userList_treatsLikeWildcardsInKeywordLiterally() throws Exception {
        UserAccount reader = createAccount(Role.USER);

        mockMvc.perform(get("/admin/users").param("keyword", "%").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(reader.getEmail()))));
    }

    @Test
    void ban_thenUnban_changesStatus_andUnbanClearsTemporaryLock() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        reader.setFailedAttempts(3);
        reader.setLockedUntil(Instant.now().plusSeconds(600));
        userAccountRepository.saveAndFlush(reader);
        AppUserPrincipal admin = principalOf(Role.ADMIN);

        mockMvc.perform(post("/admin/users/{id}/ban", reader.getId()).with(user(admin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        assertThat(reload(reader).getStatus()).isEqualTo(UserStatus.BANNED);

        mockMvc.perform(post("/admin/users/{id}/unban", reader.getId()).with(user(admin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"));
        UserAccount unbanned = reload(reader);
        assertThat(unbanned.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(unbanned.getLockedUntil()).isNull();
        assertThat(unbanned.getFailedAttempts()).isZero();
    }

    @Test
    void admin_cannotBanOwnAccount() throws Exception {
        UserAccount adminAccount = createAccount(Role.ADMIN);
        AppUserPrincipal admin = AppUserPrincipal.from(adminAccount, Instant.now());

        mockMvc.perform(post("/admin/users/{id}/ban", adminAccount.getId()).with(user(admin)).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(reload(adminAccount).getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void ban_ofUnknownAccount_reportsErrorInsteadOfFailing() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/ban", Long.MAX_VALUE).with(user(principalOf(Role.ADMIN))).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
    }
}
