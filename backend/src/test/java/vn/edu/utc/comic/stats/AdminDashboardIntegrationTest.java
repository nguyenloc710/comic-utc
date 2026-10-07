package vn.edu.utc.comic.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.stats.dto.AdminChartsResponse;
import vn.edu.utc.comic.stats.dto.AdminOverviewResponse;
import vn.edu.utc.comic.stats.dto.NamedCount;
import vn.edu.utc.comic.stats.service.AdminStatsService;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Trang tổng quan quản trị. Cơ sở dữ liệu dùng chung nên chỉ so sánh trước / sau thay vì khẳng định con số tuyệt đối.
 */
class AdminDashboardIntegrationTest extends AbstractIntegrationTest {

    private static final int CHART_DAYS = 30;

    @Autowired
    private AdminStatsService adminStatsService;

    @Test
    void overview_countsOnlyWhatReadersCanSee() {
        AdminOverviewResponse before = adminStatsService.getOverview();
        Story comic = createStory(StoryType.COMIC, story -> addGenres(story, "hanh-dong"));
        createChapter(comic, 1, ChapterStatus.PUBLISHED);
        createChapter(comic, 2, ChapterStatus.DRAFT);
        createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.HIDDEN));
        createAccount(Role.AUTHOR);
        jdbcTemplate.update("INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES (?, ?, ?)",
                comic.getId(), LocalDate.now(DateTimeConstants.DISPLAY_ZONE), 5);

        AdminOverviewResponse after = adminStatsService.getOverview();

        assertThat(after.comicCount()).isEqualTo(before.comicCount() + 1);
        assertThat(after.novelCount()).isEqualTo(before.novelCount());
        assertThat(after.publishedChapterCount()).isEqualTo(before.publishedChapterCount() + 1);
        // Mỗi truyện tạo ra một tài khoản tác giả mới, cộng thêm một tác giả tạo riêng
        assertThat(after.authorCount()).isEqualTo(before.authorCount() + 3);
        assertThat(after.userCount()).isEqualTo(before.userCount() + 3);
        assertThat(after.viewsToday()).isEqualTo(before.viewsToday() + 5);
    }

    @Test
    void charts_coverThirtyDays_andCountPublicStoriesPerGenre() {
        AdminChartsResponse before = adminStatsService.getCharts();
        createStory(StoryType.NOVEL, story -> addGenres(story, "trinh-tham"));
        createAccount(Role.USER);

        AdminChartsResponse after = adminStatsService.getCharts();

        assertThat(after.registrations()).hasSize(CHART_DAYS);
        assertThat(after.views()).hasSize(CHART_DAYS);
        assertThat(after.registrations().getLast().views()).isGreaterThan(before.registrations().getLast().views());
        assertThat(genreCount(after, "Trinh thám")).isEqualTo(genreCount(before, "Trinh thám") + 1);
        assertThat(after.topStories()).isNotEmpty();
    }

    @Test
    void dashboardPage_andChartApi_areForAdmins() throws Exception {
        mockMvc.perform(get("/admin").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_DASHBOARD));
        mockMvc.perform(get("/api/admin/stats/charts").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.views.length()").value(CHART_DAYS))
                .andExpect(jsonPath("$.data.genres").isArray());
        mockMvc.perform(get("/api/admin/stats/charts").with(user(principalOf(Role.AUTHOR))))
                .andExpect(status().isForbidden());
    }

    private static long genreCount(AdminChartsResponse charts, String genreName) {
        return charts.genres().stream()
                .filter(item -> item.name().equals(genreName))
                .mapToLong(NamedCount::count)
                .findFirst().orElse(0);
    }
}
