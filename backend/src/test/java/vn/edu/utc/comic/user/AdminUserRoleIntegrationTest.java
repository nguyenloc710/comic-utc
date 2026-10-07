package vn.edu.utc.comic.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.user.service.AdminUserService;
import org.springframework.mock.web.MockHttpSession;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

/**
 * Đổi vai trò tài khoản và hai hàng rào: không tự đổi mình, không làm hệ thống hết quản trị viên.
 */
class AdminUserRoleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AdminUserService adminUserService;

    @Test
    void changeRole_takesEffectInTheUsersCurrentSession() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        MockHttpSession readerSession = login(reader);
        mockMvc.perform(get("/studio").session(readerSession)).andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users/{id}/role", reader.getId()).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("role", "AUTHOR"))
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        assertThat(reload(reader).getRole()).isEqualTo(Role.AUTHOR);
        mockMvc.perform(get("/studio").session(readerSession)).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'USER_ROLE_CHANGED' AND entity_id = ?", Long.class,
                String.valueOf(reader.getId()))).isEqualTo(1);
    }

    @Test
    void admin_cannotChangeTheirOwnRole() throws Exception {
        UserAccount admin = createAccount(Role.ADMIN);

        mockMvc.perform(post("/admin/users/{id}/role", admin.getId()).with(csrf()).with(user(principalOf(admin)))
                        .param("role", "USER"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(reload(admin).getRole()).isEqualTo(Role.ADMIN);
    }

    /**
     * Qua giao diện, người thao tác luôn là một quản trị viên đang hoạt động khác nên "quản trị viên cuối cùng"
     * chỉ xảy ra khi tự thao tác lên mình (đã bị chặn riêng). Hàng rào vẫn phải có ở service — ví dụ khi tài khoản
     * thao tác vừa bị khóa ở một tab khác — nên ở đây gọi thẳng service với ngữ cảnh bảo mật của một quản trị viên
     * đã bị khóa.
     */
    @Test
    void theLastActiveAdmin_canNeitherBeDemotedNorBanned() {
        UserAccount lastAdmin = createAccount(Role.ADMIN);
        UserAccount bannedAdmin = createAccount(Role.ADMIN);
        // Bảng dùng chung: tạm khóa mọi quản trị viên khác để chỉ còn một người hoạt động
        jdbcTemplate.update("UPDATE user_account SET status = 'BANNED' WHERE role = 'ADMIN' AND id <> ?", lastAdmin.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principalOf(bannedAdmin), null, "ROLE_ADMIN"));
        try {
            assertThatThrownBy(() -> adminUserService.changeRole(lastAdmin.getId(), Role.USER))
                    .isInstanceOfSatisfying(ApiException.class,
                            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_LAST_ADMIN));
            assertThatThrownBy(() -> adminUserService.banUser(lastAdmin.getId()))
                    .isInstanceOfSatisfying(ApiException.class,
                            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_LAST_ADMIN));

            assertThat(reload(lastAdmin).getRole()).isEqualTo(Role.ADMIN);
            assertThat(reload(lastAdmin).getStatus()).isEqualTo(UserStatus.ACTIVE);
            // Nâng người khác lên quản trị viên thì không bị chặn
            UserAccount reader = createAccount(Role.USER);
            assertThat(adminUserService.changeRole(reader.getId(), Role.ADMIN)).isEqualTo(reader.getUsername());
            assertThat(reload(reader).getRole()).isEqualTo(Role.ADMIN);
        } finally {
            SecurityContextHolder.clearContext();
            jdbcTemplate.update("UPDATE user_account SET status = 'ACTIVE' WHERE role = 'ADMIN'");
        }
    }
}
