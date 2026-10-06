package vn.edu.utc.comic;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Phân quyền theo khu vực URL và việc ba layout hiển thị được với đúng vai trò.
 */
class SecurityAccessIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN_URL_PATTERN = "**/login";


    @Test
    void home_isOpenToGuests() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.HOME))
                .andExpect(content().string(Matchers.containsString("Comic UTC")));
    }

    @Test
    void loginPage_rendersForGuests() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.AUTH_LOGIN));
    }

    @Test
    void unknownPath_returnsApplicationErrorPageInsteadOfLoginRedirect() throws Exception {
        mockMvc.perform(get("/khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(view().name(ViewConstants.ERROR_PAGE));
    }

    @Test
    void health_isOpenToGuests() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedAreas_redirectGuestsToLogin() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(redirectedUrlPattern(LOGIN_URL_PATTERN));
        mockMvc.perform(get("/studio")).andExpect(redirectedUrlPattern(LOGIN_URL_PATTERN));
        mockMvc.perform(get("/me/library")).andExpect(redirectedUrlPattern(LOGIN_URL_PATTERN));
        mockMvc.perform(get("/actuator/env")).andExpect(redirectedUrlPattern(LOGIN_URL_PATTERN));
    }

    @Test
    void api_answersGuestsWithJsonInsteadOfLoginRedirect() throws Exception {
        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void studio_rejectsReaderAndAdmin() throws Exception {
        mockMvc.perform(get("/studio").with(user(principalOf(Role.USER))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/studio").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_rejectsReaderAndAuthor() throws Exception {
        mockMvc.perform(get("/admin").with(user(principalOf(Role.USER))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").with(user(principalOf(Role.AUTHOR))))
                .andExpect(status().isForbidden());
    }

    @Test
    void studio_rendersLayoutForAuthor() throws Exception {
        mockMvc.perform(get("/studio").with(user(principalOf(Role.AUTHOR))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_DASHBOARD))
                .andExpect(content().string(Matchers.containsString("consoleSidebar")));
    }

    @Test
    void admin_rendersLayoutForAdmin() throws Exception {
        mockMvc.perform(get("/admin").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_DASHBOARD))
                .andExpect(content().string(Matchers.containsString("consoleSidebar")));
    }

    @Test
    void readerArea_isOpenToAuthorAndAdminThroughRoleHierarchy() throws Exception {
        // Khu vực /me yêu cầu vai trò USER; tác giả và quản trị viên vào được nhờ phân cấp vai trò
        mockMvc.perform(get("/me/library").with(user(principalOf(Role.AUTHOR))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ME_LIBRARY));
        mockMvc.perform(get("/me/library").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void login_succeedsWithSeededAdminAccount() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("Admin@123"))
                .andExpect(authenticated().withUsername("admin"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void login_failsWithWrongPassword() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("sai-mat-khau"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=bad_credentials"));
    }

    @Test
    void post_withoutCsrfToken_isForbidden() throws Exception {
        mockMvc.perform(post("/logout").with(user(principalOf(Role.USER))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(user(principalOf(Role.USER))).with(csrf()))
                .andExpect(redirectedUrl("/"));
    }
}
