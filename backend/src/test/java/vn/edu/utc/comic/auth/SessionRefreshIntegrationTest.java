package vn.edu.utc.comic.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpSession;
import vn.edu.utc.comic.common.security.AccountChangedEvent;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Phiên đang đăng nhập phải theo kịp thay đổi của tài khoản mà không cần đăng nhập lại (docs/00 §4.1).
 */
class SessionRefreshIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void readerPromotedToAuthor_entersStudioWithoutLoggingInAgain() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        MockHttpSession session = login(reader);
        mockMvc.perform(get("/studio").session(session)).andExpect(status().isForbidden());

        // Giai đoạn 4 sẽ làm việc này trong AuthorRequestService.approve; ở đây chỉ kiểm chứng cơ chế làm mới phiên
        reader.setRole(Role.AUTHOR);
        userAccountRepository.saveAndFlush(reader);
        eventPublisher.publishEvent(new AccountChangedEvent(reader.getId()));

        mockMvc.perform(get("/studio").session(session)).andExpect(status().isOk());
        // Quyền mới đã được lưu vào phiên, không phải chỉ có hiệu lực cho một request
        mockMvc.perform(get("/studio").session(session)).andExpect(status().isOk());
    }

    @Test
    void bannedUser_isLoggedOutOnTheNextRequest_withTheReasonShown() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        MockHttpSession session = login(reader);
        mockMvc.perform(get("/me/profile").session(session)).andExpect(status().isOk());

        banAsAdmin(reader);

        mockMvc.perform(get("/me/profile").session(session)).andExpect(redirectedUrl("/login?error=banned"));
        // Phiên đã bị hủy hẳn: request sau đó là của một khách vãng lai
        mockMvc.perform(get("/me/profile").session(session)).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void bannedUser_callingApi_receivesJson401_notARedirect() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        MockHttpSession session = login(reader);

        banAsAdmin(reader);

        mockMvc.perform(get("/api/chat/conversations").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void renamedUser_seesNewDisplayNameInTheSameSession() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        MockHttpSession session = login(reader);
        String newName = "Tên Mới " + uniqueSuffix();

        mockMvc.perform(post("/me/profile").session(session).with(csrf()).param("displayName", newName))
                .andExpect(redirectedUrl("/me/profile"));

        mockMvc.perform(get("/").session(session))
                .andExpect(content().string(Matchers.containsString(newName)));
    }

    private void banAsAdmin(UserAccount target) throws Exception {
        mockMvc.perform(post("/admin/users/{id}/ban", target.getId())
                        .with(user(principalOf(Role.ADMIN))).with(csrf()))
                .andExpect(redirectedUrl("/admin/users"));
    }
}
