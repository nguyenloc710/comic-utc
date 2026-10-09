package vn.edu.utc.comic.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Rà soát bảo mật giai đoạn 7: CSRF trên form, API và đăng nhập; công cụ cho lập trình viên; header bảo mật;
 * đường dẫn ảnh không thoát ra ngoài thư mục lưu. Phân quyền theo khu vực URL đã có ở SecurityAccessIntegrationTest;
 * quyền theo dữ liệu (tác giả chỉ sửa truyện của mình) nằm ở test của từng chức năng vì phải dựng dữ liệu riêng.
 */
class SecurityRulesIntegrationTest extends AbstractIntegrationTest {

    @Test
    void stateChangingRequests_withoutCsrfToken_areRejected_andChangeNothing() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        UserAccount reader = createAccount(Role.USER);

        mockMvc.perform(put("/api/stories/{id}/follow", story.getId()).with(user(principalOf(reader))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/me/profile").with(user(principalOf(reader))).param("displayName", "Đổi tên"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/login").param("username", reader.getUsername()).param("password", ACCOUNT_PASSWORD))
                .andExpect(status().isForbidden());

        assertThat(storyCounter(story, "follow_count")).isZero();
        assertThat(reload(reader).getDisplayName()).isEqualTo(reader.getDisplayName());
    }

    @Test
    void guests_getJson401_fromApi_butMayReadComments() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/comments").param("storyId", story.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void adminApi_rejectsAuthors() throws Exception {
        expectStatus("/api/admin/stats/charts", createAccount(Role.AUTHOR), 403);
    }

    @Test
    void developerTools_areAdminOnly() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        UserAccount admin = createAccount(Role.ADMIN);

        expectStatus("/v3/api-docs", reader, 403);
        expectStatus("/v3/api-docs", admin, 200);
        mockMvc.perform(get("/actuator/info")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void pages_carrySecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", SecurityConstants.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/media/../application.yml", "/media/%2e%2e/application.yml",
            "/media/..%2fapplication.yml", "/media/%2e%2e%2f%2e%2e%2fpom.xml"})
    void mediaUrls_cannotEscapeTheStorageFolder(String path) throws Exception {
        int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();

        assertThat(status).isBetween(400, 499);
    }

    private void expectStatus(String path, UserAccount account, int expected) throws Exception {
        mockMvc.perform(get(path).with(user(principalOf(account))).with(csrf()))
                .andExpect(status().is(expected));
    }
}
