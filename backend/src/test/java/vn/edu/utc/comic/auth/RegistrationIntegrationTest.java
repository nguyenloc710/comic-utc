package vn.edu.utc.comic.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

class RegistrationIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_PASSWORD = "Matkhau123";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_createsReaderAccountAndLogsIn() throws Exception {
        String username = "docgia_" + uniqueSuffix();

        mockMvc.perform(registerRequest(username, username.toUpperCase() + "@Mail.Test", VALID_PASSWORD,
                        VALID_PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(username).withRoles(Role.USER.name()));

        UserAccount saved = userAccountRepository.findByUsernameOrEmail(username).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getEmail()).isEqualTo(username + "@mail.test");
        assertThat(saved.getPasswordHash()).isNotEqualTo(VALID_PASSWORD);
        assertThat(passwordEncoder.matches(VALID_PASSWORD, saved.getPasswordHash())).isTrue();
    }

    @Test
    void register_changesSessionId_soAPreSetSessionCannotBeHijacked() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/register"))
                .andReturn().getRequest().getSession();
        String sessionIdBeforeLogin = session.getId();
        String username = "docgia_" + uniqueSuffix();

        mockMvc.perform(registerRequest(username, username + "@mail.test", VALID_PASSWORD, VALID_PASSWORD)
                        .session(session))
                .andExpect(authenticated().withUsername(username));

        assertThat(session.getId()).isNotEqualTo(sessionIdBeforeLogin);
    }

    @Test
    void register_reportsEveryProblemAtOnce_andCreatesNothing() throws Exception {
        UserAccount existing = createAccount(Role.USER);
        long accountsBefore = userAccountRepository.count();

        // Tên đăng nhập khác hoa thường với tài khoản đã có vẫn phải bị coi là trùng
        mockMvc.perform(registerRequest(existing.getUsername().toUpperCase(), existing.getEmail(), "yeu", "khac"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.AUTH_REGISTER))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM,
                        "username", "email", "password", "confirmPassword"))
                .andExpect(unauthenticated());

        assertThat(userAccountRepository.count()).isEqualTo(accountsBefore);
    }

    @Test
    void register_rejectsUsernameThatCouldCollideWithAnEmail() throws Exception {
        mockMvc.perform(registerRequest("ai.do@mail.test", "moi_" + uniqueSuffix() + "@mail.test", VALID_PASSWORD,
                        VALID_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "username"));
    }

    @Test
    void register_rejectsPasswordWithoutDigit() throws Exception {
        mockMvc.perform(registerRequest("docgia_" + uniqueSuffix(), "moi_" + uniqueSuffix() + "@mail.test",
                        "chitoanchucai", "chitoanchucai"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode(ViewConstants.ATTR_FORM, "password",
                        "error.password.weak"));
    }

    @Test
    void registerPage_isShownToGuests_andSkippedForLoggedInUsers() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.AUTH_REGISTER));
        mockMvc.perform(get("/register").with(user(principalOf(Role.USER))))
                .andExpect(redirectedUrl("/"));
    }

    private static MockHttpServletRequestBuilder registerRequest(String username, String email, String password,
                                                                 String confirmPassword) {
        return post("/register").with(csrf())
                .param("username", username)
                .param("email", email)
                .param("displayName", "Độc Giả Mới")
                .param("password", password)
                .param("confirmPassword", confirmPassword);
    }
}
