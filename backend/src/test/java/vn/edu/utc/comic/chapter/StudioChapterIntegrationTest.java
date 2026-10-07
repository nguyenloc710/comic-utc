package vn.edu.utc.comic.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Soạn chương trong khu vực tác giả: tạo, sửa, làm sạch nội dung, đăng ngay, hẹn giờ, hủy hẹn, xóa bản nháp.
 */
class StudioChapterIntegrationTest extends AbstractIntegrationTest {

    private static final String XSS_CONTENT = "<p onclick=\"steal()\">Xin chào <strong>bạn đọc</strong></p>"
            + "<script>alert('xss')</script><img src=x onerror=alert(1)>"
            + "<p><a href=\"javascript:alert(1)\">bấm vào đây</a></p>";

    @Test
    void createNovelChapter_savesSanitizedDraft_thatOnlyTheAuthorCanPreview() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(saveChapter("/studio/stories/" + story.getId() + "/chapters", story.getAuthor(), 1, "SAVE")
                        .param("title", "  Mở đầu  ")
                        .param("contentHtml", XSS_CONTENT))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Map<String, Object> chapter = chapterRow(story, 1);
        assertThat(chapter.get("status")).isEqualTo("DRAFT");
        assertThat(chapter.get("title")).isEqualTo("Mở đầu");
        assertThat((Integer) chapter.get("word_count")).isEqualTo(7);
        // Thẻ định dạng được giữ; script, ảnh, link và mọi thuộc tính bị loại ngay lúc lưu
        assertThat(contentOf(chapter)).isEqualTo("<p>Xin chào <strong>bạn đọc</strong></p><p>bấm vào đây</p>");
        assertThat(storyCounter(story, "chapter_count")).isZero();
        String readUrl = "/stories/" + story.getSlug() + "/chapters/1";
        mockMvc.perform(get(readUrl)).andExpect(status().isNotFound());
        mockMvc.perform(get(readUrl).with(user(principalOf(story.getAuthor())))).andExpect(status().isOk());
    }

    @Test
    void newChapterForm_suggestsNextNumber_andEditorMatchesStoryType() throws Exception {
        Story novel = createPublishedStory(StoryType.NOVEL);
        createChapter(novel, 1, ChapterStatus.PUBLISHED);
        createChapter(novel, 4, ChapterStatus.DRAFT);
        Story comic = createPublishedStory(StoryType.COMIC);

        mockMvc.perform(get("/studio/stories/{id}/chapters/new", novel.getId()).with(user(principalOf(novel.getAuthor()))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_CHAPTER_NOVEL_EDITOR))
                .andExpect(content().string(containsString("name=\"chapterNo\" value=\"5\"")));
        mockMvc.perform(get("/studio/stories/{id}/chapters/new", comic.getId()).with(user(principalOf(comic.getAuthor()))))
                .andExpect(view().name(ViewConstants.STUDIO_CHAPTER_COMIC_EDITOR))
                .andExpect(content().string(containsString("name=\"chapterNo\" value=\"1\"")));
        mockMvc.perform(get("/studio/stories/{id}/chapters", novel.getId()).with(user(principalOf(novel.getAuthor()))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_CHAPTERS))
                .andExpect(content().string(containsString("Tên chương 4")));
    }

    @Test
    void chapterNumber_mustBeUniqueWithinTheStory_andInRange() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.DRAFT);
        String url = "/studio/stories/" + story.getId() + "/chapters";

        mockMvc.perform(saveChapter(url, story.getAuthor(), 1, "SAVE").param("contentHtml", "<p>Trùng số</p>"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_CHAPTER_NOVEL_EDITOR))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "chapterNo"));
        mockMvc.perform(saveChapter(url, story.getAuthor(), 0, "SAVE"))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "chapterNo"));

        assertThat(countChapters(story)).isEqualTo(1);
    }

    @Test
    void publishButton_savesThenPublishes_andCountsTheChapterOnTheStory() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(saveChapter("/studio/stories/" + story.getId() + "/chapters", story.getAuthor(), 1, "PUBLISH")
                        .param("contentHtml", "<p>Chương đầu tiên</p>"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Map<String, Object> chapter = chapterRow(story, 1);
        assertThat(chapter.get("status")).isEqualTo("PUBLISHED");
        assertThat(chapter.get("published_at")).isNotNull();
        assertThat(storyCounter(story, "chapter_count")).isEqualTo(1);
        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<p>Chương đầu tiên</p>")));
    }

    @Test
    void publishButton_onEmptyChapter_keepsTheDraftAndExplainsWhy() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(saveChapter("/studio/stories/" + story.getId() + "/chapters", story.getAuthor(), 1, "PUBLISH")
                        .param("contentHtml", "<p><br></p>"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        // Phần đã soạn (số chương) vẫn được lưu; chỉ bước đăng bị từ chối
        assertThat(chapterRow(story, 1).get("status")).isEqualTo("DRAFT");
        assertThat(storyCounter(story, "chapter_count")).isZero();
    }

    @Test
    void schedule_storesVietnamTimeAsUtc_rejectsThePast_andCanBeCancelled() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Chapter chapter = createChapter(story, 1, ChapterStatus.DRAFT);
        String url = "/studio/chapters/" + chapter.getId();

        mockMvc.perform(saveChapter(url, story.getAuthor(), 1, "SCHEDULE").param("contentHtml", "<p>Nội dung</p>")
                        .param("scheduledAt", "2020-01-01T08:00"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
        assertThat(chapterRow(story, 1).get("status")).isEqualTo("DRAFT");

        mockMvc.perform(saveChapter(url, story.getAuthor(), 1, "SCHEDULE").param("contentHtml", "<p>Nội dung</p>")
                        .param("scheduledAt", "2099-01-01T08:00"))
                .andExpect(redirectedUrl(url + "/edit"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        Chapter scheduled = chapterRepository.findById(chapter.getId()).orElseThrow();
        assertThat(scheduled.getStatus()).isEqualTo(ChapterStatus.SCHEDULED);
        // 08:00 giờ Việt Nam (UTC+7) là 01:00 UTC
        assertThat(scheduled.getScheduledAt()).isEqualTo(Instant.parse("2099-01-01T01:00:00Z"));
        mockMvc.perform(get(url + "/edit").with(user(principalOf(story.getAuthor()))))
                .andExpect(content().string(containsString("2099-01-01T08:00")));
        // Chương hẹn giờ chưa tới tay người đọc và chưa được tính vào số chương
        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug())).andExpect(status().isNotFound());
        assertThat(storyCounter(story, "chapter_count")).isZero();

        mockMvc.perform(post(url + "/unschedule").with(csrf()).with(user(principalOf(story.getAuthor()))))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));
        Chapter cancelled = chapterRepository.findById(chapter.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ChapterStatus.DRAFT);
        assertThat(cancelled.getScheduledAt()).isNull();
    }

    @Test
    void publishedChapter_keepsItsNumber_andCannotBeEmptied_butContentIsEditable() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Chapter chapter = createChapter(story, 1, ChapterStatus.PUBLISHED);
        String url = "/studio/chapters/" + chapter.getId();

        mockMvc.perform(saveChapter(url, story.getAuthor(), 2, "SAVE").param("contentHtml", "<p>Đổi số</p>"))
                .andExpect(view().name(ViewConstants.STUDIO_CHAPTER_NOVEL_EDITOR))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "chapterNo"));
        mockMvc.perform(saveChapter(url, story.getAuthor(), 1, "SAVE").param("contentHtml", "<p> </p>"))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "contentHtml"));
        // Đã đăng rồi thì không đăng lại được lần nữa (không cộng số chương hai lần)
        mockMvc.perform(saveChapter(url, story.getAuthor(), 1, "PUBLISH").param("contentHtml", "<p>Bản sửa lỗi chính tả</p>"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        Map<String, Object> row = chapterRow(story, 1);
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        assertThat(contentOf(row)).isEqualTo("<p>Bản sửa lỗi chính tả</p>");
        assertThat(storyCounter(story, "chapter_count")).isZero();
    }

    @Test
    void author_cannotTouchChaptersOfAnotherAuthorsStory() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Chapter chapter = createChapter(story, 1, ChapterStatus.DRAFT);
        UserAccount intruder = createAccount(Role.AUTHOR);
        String chapterUrl = "/studio/chapters/" + chapter.getId();

        mockMvc.perform(get("/studio/stories/{id}/chapters", story.getId()).with(user(principalOf(intruder))))
                .andExpect(status().isNotFound());
        mockMvc.perform(saveChapter("/studio/stories/" + story.getId() + "/chapters", intruder, 9, "SAVE"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(chapterUrl + "/edit").with(user(principalOf(intruder)))).andExpect(status().isNotFound());
        mockMvc.perform(saveChapter(chapterUrl, intruder, 1, "PUBLISH").param("contentHtml", "<p>Chiếm chương</p>"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(chapterUrl + "/delete").with(csrf()).with(user(principalOf(intruder))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(chapterRow(story, 1).get("status")).isEqualTo("DRAFT");
        assertThat(countChapters(story)).isEqualTo(1);
    }

    @Test
    void onlyDraftChapters_canBeDeleted_togetherWithTheirContent() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter draft = createChapter(story, 1, ChapterStatus.DRAFT);
        Chapter published = createChapter(story, 2, ChapterStatus.PUBLISHED);

        mockMvc.perform(post("/studio/chapters/{id}/delete", published.getId()).with(csrf())
                        .with(user(principalOf(story.getAuthor()))))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
        mockMvc.perform(post("/studio/chapters/{id}/delete", draft.getId()).with(csrf())
                        .with(user(principalOf(story.getAuthor()))))
                .andExpect(redirectedUrl("/studio/stories/" + story.getId() + "/chapters"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        assertThat(chapterRepository.existsById(published.getId())).isTrue();
        assertThat(chapterRepository.existsById(draft.getId())).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chapter_page WHERE chapter_id = ?", Long.class,
                draft.getId())).isZero();
    }

    private MockHttpServletRequestBuilder saveChapter(String url, UserAccount author, int chapterNo, String action) {
        return post(url).with(csrf()).with(user(principalOf(author)))
                .param("chapterNo", String.valueOf(chapterNo))
                .param("action", action);
    }

    private Map<String, Object> chapterRow(Story story, int chapterNo) {
        return jdbcTemplate.queryForMap("SELECT * FROM chapter WHERE story_id = ? AND chapter_no = ?", story.getId(),
                chapterNo);
    }

    private String contentOf(Map<String, Object> chapterRow) {
        return jdbcTemplate.queryForObject("SELECT content FROM chapter_content WHERE chapter_id = ?", String.class,
                chapterRow.get("id"));
    }

    private long countChapters(Story story) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chapter WHERE story_id = ?", Long.class, story.getId());
    }
}
