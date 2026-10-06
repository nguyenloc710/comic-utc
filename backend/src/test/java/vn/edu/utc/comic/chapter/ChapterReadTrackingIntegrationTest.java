package vn.edu.utc.comic.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Đếm lượt xem (chạy nền nên phải chờ) và lưu tiến độ đọc khi mở một chương.
 */
class ChapterReadTrackingIntegrationTest extends AbstractIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);
    /** Đủ lâu để một lượt xem bị tính nhầm (nếu có) kịp ghi xuống trước khi khẳng định là không có. */
    private static final Duration SETTLE_TIME = Duration.ofMillis(700);

    @Test
    void view_isCountedOncePerVisitorPerChapter() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Chapter chapter = createChapter(story, 1, ChapterStatus.PUBLISHED);
        String url = "/stories/" + story.getSlug() + "/chapters/1";
        MockHttpSession firstVisitor = new MockHttpSession();

        mockMvc.perform(get(url).session(firstVisitor)).andExpect(status().isOk());
        mockMvc.perform(get(url).session(firstVisitor)).andExpect(status().isOk());
        awaitStoryViews(story, 1);

        mockMvc.perform(get(url).session(new MockHttpSession())).andExpect(status().isOk());
        awaitStoryViews(story, 2);

        // Ba bộ đếm được ghi bằng ba câu lệnh nối tiếp nhau nên cũng phải chờ hai bộ đếm còn lại
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            assertThat(chapterViews(chapter)).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT SUM(view_count) FROM story_view_daily WHERE story_id = ?", Long.class, story.getId()))
                    .isEqualTo(2);
        });
    }

    @Test
    void sameVisitor_isCountedSeparatelyForDifferentChapters() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        createChapter(story, 1, ChapterStatus.PUBLISHED);
        createChapter(story, 2, ChapterStatus.PUBLISHED);
        MockHttpSession visitor = new MockHttpSession();

        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug()).session(visitor));
        mockMvc.perform(get("/stories/{slug}/chapters/2", story.getSlug()).session(visitor));

        awaitStoryViews(story, 2);
    }

    @Test
    void authorReadingOwnStory_andPreviewOfUnpublishedChapter_areNotCounted() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.PUBLISHED);
        createChapter(story, 2, ChapterStatus.DRAFT);
        AppUserPrincipal author = principalOf(story.getAuthor());

        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug()).with(user(author))).andExpect(status().isOk());
        mockMvc.perform(get("/stories/{slug}/chapters/2", story.getSlug()).with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk());

        await().during(SETTLE_TIME).atMost(ASYNC_TIMEOUT).until(() -> storyCounter(story, "view_count") == 0);
    }

    @Test
    void readingProgress_followsLatestOpenedChapter_andFeedsContinueReading() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.PUBLISHED);
        Chapter second = createChapter(story, 2, ChapterStatus.PUBLISHED);
        UserAccount reader = createAccount(Role.USER);
        AppUserPrincipal principal = principalOf(reader);

        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug()).with(user(principal)));
        mockMvc.perform(get("/stories/{slug}/chapters/2", story.getSlug()).with(user(principal)));

        assertThat(jdbcTemplate.queryForList("SELECT chapter_id FROM reading_history WHERE user_id = ? AND story_id = ?",
                Long.class, reader.getId(), story.getId())).containsExactly(second.getId());
        // Lưu tiến độ và đếm lượt xem chạy song song trên cùng dòng truyện: không bên nào được làm bên kia hỏng
        awaitStoryViews(story, 2);
        String continueUrl = "/stories/" + story.getSlug() + "/chapters/2";
        mockMvc.perform(get("/stories/{slug}", story.getSlug()).with(user(principal)))
                .andExpect(content().string(containsString("Đọc tiếp chương 2")));
        mockMvc.perform(get("/me/history").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(story.getTitle())))
                .andExpect(content().string(containsString(continueUrl)));
        mockMvc.perform(get("/").with(user(principal)))
                .andExpect(content().string(containsString(continueUrl)));
    }

    @Test
    void guestReading_recordsNoProgress() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.PUBLISHED);

        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug())).andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reading_history WHERE story_id = ?",
                Long.class, story.getId())).isZero();
    }

    private void awaitStoryViews(Story story, long expected) {
        await().atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> assertThat(storyCounter(story, "view_count")).isEqualTo(expected));
    }

    private long chapterViews(Chapter chapter) {
        return jdbcTemplate.queryForObject("SELECT view_count FROM chapter WHERE id = ?", Long.class, chapter.getId());
    }
}
