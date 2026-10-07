package vn.edu.utc.comic.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.report.entity.Report;
import vn.edu.utc.comic.report.enums.ReportStatus;
import vn.edu.utc.comic.report.repository.ReportRepository;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Báo cáo vi phạm: độc giả gửi qua API, quản trị viên xử lý trong hàng đợi.
 */
class ReportIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void reader_reportsAStory_onceWhilePending() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        UserAccount reader = createAccount(Role.USER);

        submit(reader, "STORY", story.getId(), "SPAM", "  Toàn quảng cáo  ").andExpect(status().isCreated());
        submit(reader, "STORY", story.getId(), "SPAM", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REPORT_ALREADY_PENDING"));
        // Người khác vẫn báo cáo được
        submit(createAccount(Role.USER), "STORY", story.getId(), "COPYRIGHT", null).andExpect(status().isCreated());

        Report report = reportRepository.findAll().stream()
                .filter(found -> found.getReporter().getId().equals(reader.getId()))
                .findFirst().orElseThrow();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getDetail()).isEqualTo("Toàn quảng cáo");
        assertThat(countReportsOn(story)).isEqualTo(2);
    }

    @Test
    void report_isRefused_forGuests_invalidInput_andContentReadersCannotSee() throws Exception {
        Story draft = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.DRAFT));
        Story published = createPublishedStory(StoryType.NOVEL);
        Chapter draftChapter = createChapter(published, 1, ChapterStatus.DRAFT);
        UserAccount reader = createAccount(Role.USER);

        mockMvc.perform(post("/api/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("STORY", published.getId(), "SPAM", null)))
                .andExpect(status().isUnauthorized());
        submit(reader, "STORY", draft.getId(), "SPAM", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REPORT_TARGET_NOT_FOUND"));
        submit(reader, "CHAPTER", draftChapter.getId(), "SPAM", null).andExpect(status().isNotFound());
        submit(reader, "COMMENT", Long.MAX_VALUE, "SPAM", null).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/reports").with(csrf()).with(user(principalOf(reader)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\": \"STORY\", \"targetId\": %d}".formatted(published.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));

        assertThat(countReportsOn(published)).isZero();
    }

    @Test
    void admin_seesPendingReports_andCanDismissOne() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        UserAccount reader = createAccount(Role.USER);
        submit(reader, "STORY", story.getId(), "VIOLENCE", "Cảnh máu me " + uniqueSuffix());
        long reportId = latestReportId(story);

        mockMvc.perform(get("/admin/reports").with(user(principalOf(Role.USER)))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reports").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_REPORTS))
                .andExpect(content().string(containsString(story.getTitle())));
        mockMvc.perform(get("/admin/reports/{id}", reportId).with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_REPORT_DETAIL))
                .andExpect(content().string(containsString(reader.getUsername())));

        mockMvc.perform(post("/admin/reports/{id}/dismiss", reportId).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("note", "Truyện có cảnh hành động bình thường"))
                .andExpect(redirectedUrl("/admin/reports"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Report report = reportRepository.findById(reportId).orElseThrow();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(report.getHandledAt()).isNotNull();
        assertThat(report.getResolutionNote()).isEqualTo("Truyện có cảnh hành động bình thường");
        assertThat(storyVisibility(story)).isEqualTo(StoryVisibility.PUBLISHED);
        // Đã xử lý thì không xử lý lại
        mockMvc.perform(post("/admin/reports/{id}/resolve", reportId).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", "Lý do")).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
    }

    @Test
    void resolving_hidesTheReportedContentWithTheGivenReason() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Chapter chapter = createChapter(story, 1, ChapterStatus.PUBLISHED);
        UserAccount writer = createAccount(Role.USER);
        long commentId = insertComment(story, writer);
        UserAccount reader = createAccount(Role.USER);
        submit(reader, "CHAPTER", chapter.getId(), "ADULT_CONTENT", null);
        long chapterReport = latestReportId(story);
        submit(reader, "COMMENT", commentId, "HARASSMENT", null);
        long commentReport = latestReportId(story);
        String reason = "Vi phạm tiêu chuẩn cộng đồng " + uniqueSuffix();

        mockMvc.perform(post("/admin/reports/{id}/resolve", chapterReport).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", "  "))
                .andExpect(view().name(ViewConstants.ADMIN_REPORT_DETAIL));
        mockMvc.perform(post("/admin/reports/{id}/resolve", chapterReport).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", reason))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        mockMvc.perform(post("/admin/reports/{id}/resolve", commentReport).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", reason))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Chapter hiddenChapter = chapterRepository.findById(chapter.getId()).orElseThrow();
        assertThat(hiddenChapter.getStatus()).isEqualTo(ChapterStatus.HIDDEN);
        assertThat(hiddenChapter.getHiddenReason()).isEqualTo(reason);
        assertThat(jdbcTemplate.queryForMap("SELECT status, hidden_reason FROM comment WHERE id = ?", commentId))
                .containsEntry("status", "HIDDEN").containsEntry("hidden_reason", reason);
        assertThat(reportRepository.findById(chapterReport).orElseThrow().getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(reportRepository.findById(commentReport).orElseThrow().getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void resolving_aReportWhoseTargetIsAlreadyHidden_onlyClosesTheReport() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        UserAccount first = createAccount(Role.USER);
        UserAccount second = createAccount(Role.USER);
        submit(first, "STORY", story.getId(), "COPYRIGHT", null);
        long firstReport = latestReportId(story);
        submit(second, "STORY", story.getId(), "COPYRIGHT", null);
        long secondReport = latestReportId(story);

        mockMvc.perform(post("/admin/reports/{id}/resolve", firstReport).with(csrf()).with(user(principalOf(Role.ADMIN)))
                .param("reason", "Sao chép"));
        mockMvc.perform(post("/admin/reports/{id}/resolve", secondReport).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", "Sao chép"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        assertThat(storyVisibility(story)).isEqualTo(StoryVisibility.HIDDEN);
        assertThat(reportRepository.findById(secondReport).orElseThrow().getStatus()).isEqualTo(ReportStatus.RESOLVED);
        mockMvc.perform(get("/admin/reports").param("status", "RESOLVED").with(user(principalOf(Role.ADMIN))))
                .andExpect(content().string(containsString(story.getTitle())));
    }

    @Test
    void storyPage_offersTheReportButton_toLoggedInReadersOnly() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.PUBLISHED);

        mockMvc.perform(get("/stories/{slug}", story.getSlug()))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("data-report-button"))));
        mockMvc.perform(get("/stories/{slug}", story.getSlug()).with(user(principalOf(Role.USER))))
                .andExpect(content().string(containsString("data-report-button")))
                .andExpect(content().string(containsString("id=\"reportModal\"")));
        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug()).with(user(principalOf(Role.USER))))
                .andExpect(content().string(containsString("data-target-type=\"CHAPTER\"")));
        // Tác giả không tự báo cáo truyện của mình
        mockMvc.perform(get("/stories/{slug}", story.getSlug()).with(user(principalOf(story.getAuthor()))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("data-report-button"))));
    }

    private ResultActions submit(UserAccount reporter, String targetType, Long targetId, String reason, String detail)
            throws Exception {
        return mockMvc.perform(post("/api/reports").with(csrf()).with(user(principalOf(reporter)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(targetType, targetId, reason, detail)));
    }

    private String body(String targetType, Long targetId, String reason, String detail) throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("targetType", targetType);
        values.put("targetId", targetId);
        values.put("reason", reason);
        values.put("detail", detail);
        return objectMapper.writeValueAsString(values);
    }

    private long insertComment(Story story, UserAccount author) {
        jdbcTemplate.update("INSERT INTO comment (story_id, user_id, content, created_at) VALUES (?, ?, ?, ?)",
                story.getId(), author.getId(), "Bình luận bị báo cáo", utc(Instant.now()));
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM comment WHERE story_id = ?", Long.class, story.getId());
    }

    /** Báo cáo mới nhất nhắm tới truyện, chương hoặc bình luận của truyện này. */
    private long latestReportId(Story story) {
        return jdbcTemplate.queryForObject("""
                SELECT MAX(r.id) FROM report r
                WHERE (r.target_type = 'STORY' AND r.target_id = ?)
                   OR (r.target_type = 'CHAPTER' AND r.target_id IN (SELECT id FROM chapter WHERE story_id = ?))
                   OR (r.target_type = 'COMMENT' AND r.target_id IN (SELECT id FROM comment WHERE story_id = ?))
                """, Long.class, story.getId(), story.getId(), story.getId());
    }

    private long countReportsOn(Story story) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM report WHERE target_type = 'STORY' AND target_id = ?",
                Long.class, story.getId());
    }

    private StoryVisibility storyVisibility(Story story) {
        return storyRepository.findById(story.getId()).orElseThrow().getVisibility();
    }
}
