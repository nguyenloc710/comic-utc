package vn.edu.utc.comic.author;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.utc.comic.author.entity.AuthorProfile;
import vn.edu.utc.comic.author.entity.AuthorRequest;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;
import vn.edu.utc.comic.author.enums.IntendedStoryType;
import vn.edu.utc.comic.author.repository.AuthorProfileRepository;
import vn.edu.utc.comic.author.repository.AuthorRequestRepository;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Quy trình đăng ký làm tác giả: gửi yêu cầu, duyệt, từ chối và gửi lại (docs/00 §4.1).
 */
class AuthorRequestIntegrationTest extends AbstractIntegrationTest {

    private static final String INTRODUCTION = "Tôi viết truyện trinh thám đã ba năm và muốn đăng tác phẩm ở đây.";
    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private AuthorRequestRepository authorRequestRepository;

    @Autowired
    private AuthorProfileRepository authorProfileRepository;

    @Test
    void reader_submitsRequest_andThenSeesItPendingInsteadOfTheForm() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        String penName = "Bút Danh " + uniqueSuffix();

        mockMvc.perform(submit(reader, "  " + penName + "  ", INTRODUCTION, "NOVEL"))
                .andExpect(redirectedUrl("/me/author-request"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        AuthorRequest request = latestRequest(reader);
        assertThat(request.getStatus()).isEqualTo(AuthorRequestStatus.PENDING);
        assertThat(request.getPenName()).isEqualTo(penName);
        assertThat(request.getIntendedType()).isEqualTo(IntendedStoryType.NOVEL);
        mockMvc.perform(get("/me/author-request").with(user(principalOf(reader))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(penName)))
                .andExpect(content().string(containsString("Chờ duyệt")))
                .andExpect(content().string(not(containsString("name=\"penName\""))));
    }

    @Test
    void secondRequest_whileOneIsPending_isRefused() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        mockMvc.perform(submit(reader, "Lần Một " + uniqueSuffix(), INTRODUCTION, "COMIC"));

        mockMvc.perform(submit(reader, "Lần Hai " + uniqueSuffix(), INTRODUCTION, "COMIC"))
                .andExpect(redirectedUrl("/me/author-request"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(countRequests(reader)).isEqualTo(1);
    }

    @Test
    void submit_showsFieldErrors_andStoresNothing() throws Exception {
        UserAccount reader = createAccount(Role.USER);

        mockMvc.perform(post("/me/author-request").with(csrf()).with(user(principalOf(reader)))
                        .param("penName", " ")
                        .param("introduction", "Quá ngắn")
                        .param("intendedType", ""))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ME_AUTHOR_REQUEST))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "penName", "introduction",
                        "intendedType"));

        assertThat(countRequests(reader)).isZero();
    }

    @Test
    void penName_cannotDuplicateAnAuthor_orAnotherPendingRequest_evenInDifferentCase() throws Exception {
        String suffix = uniqueSuffix();
        createPendingRequest(createAccount(Role.USER), "Lam Phong " + suffix);
        UserAccount existingAuthor = createAccount(Role.AUTHOR);
        createProfile(existingAuthor, "Dạ Vũ " + suffix);
        UserAccount reader = createAccount(Role.USER);

        for (String takenPenName : new String[] {"lam phong " + suffix, "DẠ VŨ " + suffix}) {
            mockMvc.perform(submit(reader, takenPenName, INTRODUCTION, "BOTH"))
                    .andExpect(view().name(ViewConstants.ME_AUTHOR_REQUEST))
                    .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "penName"));
        }

        assertThat(countRequests(reader)).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"AUTHOR", "ADMIN"})
    void onlyReaders_maySubmit(Role role) throws Exception {
        UserAccount account = createAccount(role);

        mockMvc.perform(submit(account, "Không Được " + uniqueSuffix(), INTRODUCTION, "NOVEL"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(countRequests(account)).isZero();
        mockMvc.perform(get("/me/author-request").with(user(principalOf(account))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"penName\""))));
    }

    @Test
    void approve_makesTheReaderAnAuthor_inTheSessionTheyAreAlreadyLoggedInWith() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        String penName = "Tác Giả Mới " + uniqueSuffix();
        AuthorRequest request = createPendingRequest(reader, penName);
        MockHttpSession readerSession = login(reader);
        mockMvc.perform(get("/studio").session(readerSession)).andExpect(status().isForbidden());

        mockMvc.perform(review(request, "approve"))
                .andExpect(redirectedUrl("/admin/author-requests"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        assertThat(reload(reader).getRole()).isEqualTo(Role.AUTHOR);
        assertThat(authorProfileRepository.findById(reader.getId())).get()
                .extracting(AuthorProfile::getPenName).isEqualTo(penName);
        AuthorRequest reviewed = authorRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reviewed.getStatus()).isEqualTo(AuthorRequestStatus.APPROVED);
        assertThat(reviewed.getReviewedAt()).isNotNull();
        // Không đăng nhập lại: chính phiên cũ đã vào được khu vực tác giả
        mockMvc.perform(get("/studio").session(readerSession)).andExpect(status().isOk());
        awaitNotifications(reader, "AUTHOR_REQUEST_APPROVED", 1);
    }

    @Test
    void approve_twice_createsOnlyOneProfile() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        AuthorRequest request = createPendingRequest(reader, "Bấm Đúp " + uniqueSuffix());
        mockMvc.perform(review(request, "approve")).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        mockMvc.perform(review(request, "approve")).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(authorProfileRepository.findById(reader.getId())).isPresent();
        awaitNotifications(reader, "AUTHOR_REQUEST_APPROVED", 1);
    }

    @Test
    void approve_isRefused_whenAnotherAuthorTookThePenNameMeanwhile() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        String penName = "Trùng Tên " + uniqueSuffix();
        AuthorRequest request = createPendingRequest(reader, penName);
        createProfile(createAccount(Role.AUTHOR), penName);

        mockMvc.perform(review(request, "approve")).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(reload(reader).getRole()).isEqualTo(Role.USER);
        assertThat(authorRequestRepository.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(AuthorRequestStatus.PENDING);
    }

    @Test
    void reject_requiresAReason() throws Exception {
        AuthorRequest request = createPendingRequest(createAccount(Role.USER), "Thiếu Lý Do " + uniqueSuffix());

        mockMvc.perform(review(request, "reject").param("rejectReason", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_AUTHOR_REQUEST_DETAIL))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "rejectReason"));

        assertThat(authorRequestRepository.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(AuthorRequestStatus.PENDING);
    }

    @Test
    void rejectedReader_seesTheReason_andMayResubmitOnlyAfterTheCooldown() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        AuthorRequest request = createPendingRequest(reader, "Bị Từ Chối " + uniqueSuffix());
        String reason = "Lời giới thiệu chưa cho thấy kinh nghiệm sáng tác " + uniqueSuffix();

        mockMvc.perform(review(request, "reject").param("rejectReason", reason))
                .andExpect(redirectedUrl("/admin/author-requests"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        assertThat(reload(reader).getRole()).isEqualTo(Role.USER);
        awaitNotifications(reader, "AUTHOR_REQUEST_REJECTED", 1);
        mockMvc.perform(get("/me/author-request").with(user(principalOf(reader))))
                .andExpect(content().string(containsString(reason)))
                .andExpect(content().string(not(containsString("name=\"penName\""))));
        // Còn trong thời gian chờ (7 ngày theo dữ liệu khởi tạo): chưa gửi lại được
        mockMvc.perform(submit(reader, "Gửi Lại Sớm " + uniqueSuffix(), INTRODUCTION, "NOVEL"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
        assertThat(countRequests(reader)).isEqualTo(1);

        // Lùi thời điểm từ chối về 8 ngày trước để coi như đã hết thời gian chờ
        jdbcTemplate.update("UPDATE author_request SET reviewed_at = ? WHERE id = ?",
                utc(Instant.now().minus(8, ChronoUnit.DAYS)), request.getId());
        mockMvc.perform(get("/me/author-request").with(user(principalOf(reader))))
                .andExpect(content().string(containsString("name=\"penName\"")));
        mockMvc.perform(submit(reader, "Gửi Lại " + uniqueSuffix(), INTRODUCTION, "NOVEL"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        assertThat(latestRequest(reader).getStatus()).isEqualTo(AuthorRequestStatus.PENDING);
        assertThat(countRequests(reader)).isEqualTo(2);
    }

    @Test
    void reviewScreens_areForAdminsOnly_andListPendingRequests() throws Exception {
        String penName = "Chờ Xét " + uniqueSuffix();
        AuthorRequest request = createPendingRequest(createAccount(Role.USER), penName);

        mockMvc.perform(get("/admin/author-requests").with(user(principalOf(Role.AUTHOR))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/author-requests/{id}/approve", request.getId()).with(csrf())
                        .with(user(principalOf(Role.USER))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/author-requests").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_AUTHOR_REQUESTS))
                .andExpect(content().string(containsString(penName)));
        mockMvc.perform(get("/admin/author-requests/{id}", request.getId()).with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(INTRODUCTION)));
        mockMvc.perform(get("/admin/author-requests/{id}", Long.MAX_VALUE).with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder submit(UserAccount account, String penName, String introduction,
                                                 String intendedType) {
        return post("/me/author-request").with(csrf()).with(user(principalOf(account)))
                .param("penName", penName)
                .param("introduction", introduction)
                .param("intendedType", intendedType);
    }

    /** Quản trị viên (một tài khoản mới) duyệt hoặc từ chối yêu cầu. */
    private MockHttpServletRequestBuilder review(AuthorRequest request, String action) {
        return post("/admin/author-requests/{id}/{action}", request.getId(), action).with(csrf())
                .with(user(principalOf(Role.ADMIN)));
    }

    private AuthorRequest createPendingRequest(UserAccount user, String penName) {
        AuthorRequest request = new AuthorRequest();
        request.setUser(user);
        request.setPenName(penName);
        request.setIntroduction(INTRODUCTION);
        request.setIntendedType(IntendedStoryType.NOVEL);
        return authorRequestRepository.saveAndFlush(request);
    }

    private void createProfile(UserAccount author, String penName) {
        jdbcTemplate.update("INSERT INTO author_profile (user_id, pen_name, approved_at) VALUES (?, ?, ?)",
                author.getId(), penName, utc(Instant.now()));
    }

    private AuthorRequest latestRequest(UserAccount user) {
        return authorRequestRepository.findFirstByUserIdOrderByIdDesc(user.getId()).orElseThrow();
    }

    private long countRequests(UserAccount user) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM author_request WHERE user_id = ?", Long.class,
                user.getId());
    }

    /** Thông báo được ghi trên luồng nền sau khi transaction duyệt commit, nên phải chờ. */
    private void awaitNotifications(UserAccount recipient, String type, long expected) {
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE recipient_id = ? AND type = ?", Long.class,
                recipient.getId(), type)).isEqualTo(expected));
    }
}
