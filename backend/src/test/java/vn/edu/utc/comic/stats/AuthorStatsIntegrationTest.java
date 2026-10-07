package vn.edu.utc.comic.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.stats.dto.AuthorStatsOverview;
import vn.edu.utc.comic.stats.dto.DailyViewPoint;
import vn.edu.utc.comic.stats.dto.TopChapterResponse;
import vn.edu.utc.comic.stats.service.AuthorStatsService;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Thống kê của tác giả: chỉ tính truyện của chính người đó và bỏ qua truyện đã xóa.
 */
class AuthorStatsIntegrationTest extends AbstractIntegrationTest {

    private static final int CHART_DAYS = 30;

    @Autowired
    private AuthorStatsService authorStatsService;

    @Test
    void overview_sumsTheAuthorsLiveStoriesOnly() {
        UserAccount author = createAccount(Role.AUTHOR);
        createStoryOf(author, 100, 10, 8, 2);
        createStoryOf(author, 50, 5, 9, 2);
        createStory(StoryType.NOVEL, story -> {
            story.setAuthor(author);
            story.setViewCount(9_999);
            story.setDeletedAt(Instant.now());
        });
        createStory(StoryType.NOVEL, story -> story.setViewCount(7_777));

        AuthorStatsOverview overview = authorStatsService.getOverview(author.getId());

        assertThat(overview.storyCount()).isEqualTo(2);
        assertThat(overview.viewCount()).isEqualTo(150);
        assertThat(overview.followCount()).isEqualTo(15);
        assertThat(overview.ratingCount()).isEqualTo(4);
        assertThat(overview.ratingAverage()).isEqualTo(4.25);
    }

    @Test
    void overview_ofAnAuthorWithoutStories_isAllZeros() {
        AuthorStatsOverview overview = authorStatsService.getOverview(createAccount(Role.AUTHOR).getId());

        assertThat(overview.storyCount()).isZero();
        assertThat(overview.viewCount()).isZero();
        assertThat(overview.ratingAverage()).isNull();
    }

    @Test
    void dailyViews_coverThirtyDaysWithZerosForQuietDays() {
        UserAccount author = createAccount(Role.AUTHOR);
        Story first = createStoryOf(author, 0, 0, 0, 0);
        Story second = createStoryOf(author, 0, 0, 0, 0);
        Story someoneElses = createPublishedStory(StoryType.NOVEL);
        addDailyViews(first, 0, 7);
        addDailyViews(first, 3, 5);
        addDailyViews(second, 0, 2);
        // Quá 30 ngày và truyện của người khác đều không được tính
        addDailyViews(first, CHART_DAYS, 1_000);
        addDailyViews(someoneElses, 0, 5_000);

        List<DailyViewPoint> all = authorStatsService.getDailyViews(author.getId(), null);
        List<DailyViewPoint> onlySecond = authorStatsService.getDailyViews(author.getId(), second.getId());

        assertThat(all).hasSize(CHART_DAYS);
        assertThat(all.getLast().date()).isEqualTo(today());
        assertThat(all.getFirst().date()).isEqualTo(today().minusDays(CHART_DAYS - 1));
        assertThat(all.getLast().views()).isEqualTo(9);
        assertThat(all.get(CHART_DAYS - 4).views()).isEqualTo(5);
        assertThat(all.stream().mapToLong(DailyViewPoint::views).sum()).isEqualTo(14);
        assertThat(onlySecond.getLast().views()).isEqualTo(2);
        assertThat(onlySecond.stream().mapToLong(DailyViewPoint::views).sum()).isEqualTo(2);
        assertThatThrownBy(() -> authorStatsService.getDailyViews(author.getId(), someoneElses.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORY_NOT_FOUND));
    }

    @Test
    void topChapters_listPublishedChaptersByViews() {
        UserAccount author = createAccount(Role.AUTHOR);
        Story story = createStoryOf(author, 0, 0, 0, 0);
        setViews(createChapter(story, 1, ChapterStatus.PUBLISHED), 30);
        setViews(createChapter(story, 2, ChapterStatus.PUBLISHED), 80);
        setViews(createChapter(story, 3, ChapterStatus.DRAFT), 500);

        List<TopChapterResponse> topChapters = authorStatsService.findTopChapters(author.getId());

        assertThat(topChapters).extracting(TopChapterResponse::chapterNo).containsExactly(2, 1);
        assertThat(topChapters.getFirst().viewCount()).isEqualTo(80);
        assertThat(topChapters.getFirst().storySlug()).isEqualTo(story.getSlug());
    }

    @Test
    void statsPages_andChartApi_showTheAuthorsOwnNumbers() throws Exception {
        UserAccount author = createAccount(Role.AUTHOR);
        Story story = createStoryOf(author, 1_234, 0, 0, 0);
        Story someoneElses = createPublishedStory(StoryType.NOVEL);
        addDailyViews(story, 0, 4);

        mockMvc.perform(get("/studio").with(user(principalOf(author))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_DASHBOARD))
                .andExpect(content().string(containsString("1.234")));
        mockMvc.perform(get("/studio/stats").with(user(principalOf(author))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_STATS))
                .andExpect(content().string(containsString(story.getTitle())))
                .andExpect(content().string(not(containsString(someoneElses.getTitle()))));
        mockMvc.perform(get("/api/studio/stats/daily-views").with(user(principalOf(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(CHART_DAYS))
                .andExpect(jsonPath("$.data[" + (CHART_DAYS - 1) + "].views").value(4))
                .andExpect(jsonPath("$.data[" + (CHART_DAYS - 1) + "].date").value(today().toString()));
        mockMvc.perform(get("/api/studio/stats/daily-views").param("storyId", String.valueOf(someoneElses.getId()))
                        .with(user(principalOf(author))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/studio/stats/daily-views").with(user(principalOf(Role.USER))))
                .andExpect(status().isForbidden());
    }

    private Story createStoryOf(UserAccount author, long views, int follows, int ratingSum, int ratingCount) {
        return createStory(StoryType.NOVEL, story -> {
            story.setAuthor(author);
            story.setViewCount(views);
            story.setFollowCount(follows);
            story.setRatingSum(ratingSum);
            story.setRatingCount(ratingCount);
        });
    }

    private void addDailyViews(Story story, int daysAgo, int views) {
        jdbcTemplate.update("INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES (?, ?, ?)",
                story.getId(), today().minusDays(daysAgo), views);
    }

    private void setViews(Chapter chapter, long views) {
        jdbcTemplate.update("UPDATE chapter SET view_count = ? WHERE id = ?", views, chapter.getId());
    }

    private static LocalDate today() {
        return LocalDate.now(DateTimeConstants.DISPLAY_ZONE);
    }
}
