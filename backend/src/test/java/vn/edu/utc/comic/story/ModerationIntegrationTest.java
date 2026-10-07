package vn.edu.utc.comic.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.constant.CacheConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.stats.enums.RankingType;
import vn.edu.utc.comic.stats.service.RankingService;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Kiểm duyệt của quản trị viên: ẩn / gỡ ẩn truyện, chương, bình luận — kèm lý do, thông báo và bộ đếm đúng.
 */
class ModerationIntegrationTest extends AbstractIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration SETTLE_TIME = Duration.ofMillis(700);

    @Autowired
    private RankingService rankingService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void moderationScreens_areForAdminsOnly() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(get("/admin/stories").with(user(principalOf(Role.AUTHOR)))).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/stories/{id}/hide", story.getId()).with(csrf()).with(user(principalOf(Role.USER)))
                        .param("reason", "x")).andExpect(status().isForbidden());

        assertThat(visibilityOf(story)).isEqualTo(StoryVisibility.PUBLISHED);
    }

    @Test
    void hidingAStory_removesItFromReaders_notifiesTheAuthor_andRebuildsRankings() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        String reason = "Nội dung sao chép từ tác phẩm khác " + uniqueSuffix();
        rankingService.getRanking(RankingType.FOLLOWS);
        assertThat(rankingCache().getNativeCache().asMap()).isNotEmpty();

        mockMvc.perform(post("/admin/stories/{id}/hide", story.getId()).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", reason))
                .andExpect(redirectedUrl("/admin/stories/" + story.getId()))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Story hidden = storyRepository.findById(story.getId()).orElseThrow();
        assertThat(hidden.getVisibility()).isEqualTo(StoryVisibility.HIDDEN);
        assertThat(hidden.getHiddenReason()).isEqualTo(reason);
        assertThat(rankingCache().getNativeCache().asMap()).isEmpty();
        mockMvc.perform(get("/stories/{slug}", story.getSlug())).andExpect(status().isNotFound());
        // Quản trị viên và tác giả vẫn xem được bản bị ẩn; tác giả thấy lý do trong studio
        mockMvc.perform(get("/stories/{slug}", story.getSlug()).with(user(principalOf(Role.ADMIN)))).andExpect(status().isOk());
        mockMvc.perform(get("/studio/stories").with(user(principalOf(story.getAuthor()))))
                .andExpect(content().string(containsString(reason)));
        awaitNotifications(story.getAuthor(), 1);
        mockMvc.perform(get("/me/notifications").with(user(principalOf(story.getAuthor()))))
                .andExpect(content().string(containsString(reason)));

        // Gỡ ẩn: công khai trở lại, không báo thêm gì
        mockMvc.perform(post("/admin/stories/{id}/unhide", story.getId()).with(csrf()).with(user(principalOf(Role.ADMIN))))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        assertThat(visibilityOf(story)).isEqualTo(StoryVisibility.PUBLISHED);
        assertThat(storyRepository.findById(story.getId()).orElseThrow().getHiddenReason()).isNull();
        mockMvc.perform(get("/stories/{slug}", story.getSlug())).andExpect(status().isOk());
        await().during(SETTLE_TIME).atMost(ASYNC_TIMEOUT).until(() -> countHiddenNotifications(story.getAuthor()) == 1);
    }

    @Test
    void hidingAStory_requiresAReason_andOnlyAppliesToPublishedStories() throws Exception {
        Story draft = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.DRAFT));
        Story published = createPublishedStory(StoryType.COMIC);

        mockMvc.perform(post("/admin/stories/{id}/hide", published.getId()).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_STORY_DETAIL))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "reason"));
        mockMvc.perform(post("/admin/stories/{id}/hide", draft.getId()).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", "Lý do"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
        mockMvc.perform(post("/admin/stories/{id}/unhide", published.getId()).with(csrf()).with(user(principalOf(Role.ADMIN))))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(visibilityOf(published)).isEqualTo(StoryVisibility.PUBLISHED);
        assertThat(visibilityOf(draft)).isEqualTo(StoryVisibility.DRAFT);
    }

    @Test
    void adminStoryList_showsEveryStoryIncludingDraftsAndHidden() throws Exception {
        String marker = "kiemduyet" + uniqueSuffix();
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Nháp " + marker);
            story.setVisibility(StoryVisibility.DRAFT);
        });

        mockMvc.perform(get("/admin/stories").param("keyword", marker).with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_STORIES))
                .andExpect(content().string(containsString("Nháp " + marker)));
        mockMvc.perform(get("/admin/stories").param("keyword", marker).param("visibility", "PUBLISHED")
                        .with(user(principalOf(Role.ADMIN))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Nháp " + marker))));
    }

    @Test
    void hidingAChapter_adjustsTheStoryChapterCount_andUnhidingDoesNotNotifyAgain() throws Exception {
        Story story = createStory(StoryType.COMIC, created -> created.setChapterCount(2));
        createChapter(story, 1, ChapterStatus.PUBLISHED);
        Chapter chapter = createChapter(story, 2, ChapterStatus.PUBLISHED);
        Chapter draft = createChapter(story, 3, ChapterStatus.DRAFT);
        String reason = "Ảnh vi phạm " + uniqueSuffix();
        String hideUrl = "/admin/stories/" + story.getId() + "/chapters/";

        mockMvc.perform(post(hideUrl + chapter.getId() + "/hide").with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", reason))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Chapter hidden = chapterRepository.findById(chapter.getId()).orElseThrow();
        assertThat(hidden.getStatus()).isEqualTo(ChapterStatus.HIDDEN);
        assertThat(hidden.getHiddenReason()).isEqualTo(reason);
        assertThat(storyCounter(story, "chapter_count")).isEqualTo(1);
        mockMvc.perform(get("/stories/{slug}/chapters/2", story.getSlug())).andExpect(status().isNotFound());
        mockMvc.perform(get("/stories/{slug}/chapters/2", story.getSlug()).with(user(principalOf(story.getAuthor()))))
                .andExpect(status().isOk());
        awaitNotifications(story.getAuthor(), 1);
        // Chương nháp hay chương thiếu lý do thì không ẩn được
        mockMvc.perform(post(hideUrl + draft.getId() + "/hide").with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", reason)).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
        mockMvc.perform(post(hideUrl + chapter.getId() + "/hide").with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", "")).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        mockMvc.perform(post(hideUrl + chapter.getId() + "/unhide").with(csrf()).with(user(principalOf(Role.ADMIN))))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        assertThat(chapterRepository.findById(chapter.getId()).orElseThrow().getStatus()).isEqualTo(ChapterStatus.PUBLISHED);
        assertThat(storyCounter(story, "chapter_count")).isEqualTo(2);
        mockMvc.perform(get("/stories/{slug}/chapters/2", story.getSlug())).andExpect(status().isOk());
        await().during(SETTLE_TIME).atMost(ASYNC_TIMEOUT).until(() -> countHiddenNotifications(story.getAuthor()) == 1);
        // Gỡ ẩn không phải "chương mới": người theo dõi không nhận thông báo chương mới
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification WHERE type = 'NEW_CHAPTER' AND link = ?",
                Long.class, "/stories/" + story.getSlug() + "/chapters/2")).isZero();
    }

    @Test
    void hidingAComment_withholdsItsContent_andAdjustsTheCommentCount() throws Exception {
        Story story = createStory(StoryType.NOVEL, created -> created.setCommentCount(1));
        UserAccount writer = createAccount(Role.USER);
        String content = "Bình luận quảng cáo " + uniqueSuffix();
        long commentId = insertComment(story, writer, content);
        String reason = "Spam " + uniqueSuffix();

        mockMvc.perform(post("/admin/comments/{id}/hide", commentId).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", reason))
                .andExpect(redirectedUrl("/admin/comments"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        assertThat(storyCounter(story, "comment_count")).isZero();
        mockMvc.perform(get("/api/comments").param("storyId", String.valueOf(story.getId())))
                .andExpect(jsonPath("$.data.content[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());
        // Màn kiểm duyệt vẫn đọc được nội dung và lý do
        mockMvc.perform(get("/admin/comments").param("status", "HIDDEN").param("keyword", content)
                        .with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_COMMENTS))
                .andExpect(content().string(containsString(content)))
                .andExpect(content().string(containsString(reason)));
        awaitNotifications(writer, 1);
        mockMvc.perform(post("/admin/comments/{id}/hide", commentId).with(csrf()).with(user(principalOf(Role.ADMIN)))
                        .param("reason", reason)).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        mockMvc.perform(post("/admin/comments/{id}/unhide", commentId).with(csrf()).with(user(principalOf(Role.ADMIN))))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        assertThat(storyCounter(story, "comment_count")).isEqualTo(1);
        mockMvc.perform(get("/api/comments").param("storyId", String.valueOf(story.getId())))
                .andExpect(jsonPath("$.data.content[0].content").value(content));
    }

    private long insertComment(Story story, UserAccount author, String content) {
        jdbcTemplate.update("INSERT INTO comment (story_id, user_id, content, created_at) VALUES (?, ?, ?, ?)",
                story.getId(), author.getId(), content, utc(Instant.now()));
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM comment WHERE story_id = ?", Long.class, story.getId());
    }

    private CaffeineCache rankingCache() {
        return (CaffeineCache) cacheManager.getCache(CacheConstants.RANKINGS);
    }

    private StoryVisibility visibilityOf(Story story) {
        return storyRepository.findById(story.getId()).orElseThrow().getVisibility();
    }

    private void awaitNotifications(UserAccount recipient, long expected) {
        await().atMost(ASYNC_TIMEOUT).untilAsserted(
                () -> assertThat(countHiddenNotifications(recipient)).isEqualTo(expected));
    }

    private long countHiddenNotifications(UserAccount recipient) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE recipient_id = ? AND type = 'CONTENT_HIDDEN'", Long.class,
                recipient.getId());
    }
}
