package vn.edu.utc.comic.story;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.stats.enums.RankingType;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Các trang công khai của kho truyện và quy tắc "nội dung chưa công khai trả 404 chứ không phải 403".
 */
class StoryPagesIntegrationTest extends AbstractIntegrationTest {

    @Test
    void home_listsPublishedStories() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.HOME))
                .andExpect(content().string(containsString(story.getTitle())));
    }

    @Test
    void storyList_showsMatchingPublishedStory_butNotDraftWithSameKeyword() throws Exception {
        String marker = "trang" + uniqueSuffix();
        createStory(StoryType.NOVEL, story -> story.setTitle("Lộ Diện " + marker));
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Còn Giấu " + marker);
            story.setVisibility(StoryVisibility.DRAFT);
        });

        mockMvc.perform(get("/stories").param("keyword", marker))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STORY_LIST))
                .andExpect(content().string(containsString("Lộ Diện " + marker)))
                .andExpect(content().string(not(containsString("Còn Giấu " + marker))));
    }

    @Test
    void genrePage_listsStoriesOfGenre_andUnknownGenreGives404() throws Exception {
        Story story = createStory(StoryType.NOVEL, created -> addGenres(created, "trinh-tham"));

        mockMvc.perform(get("/genres/trinh-tham"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(story.getTitle())));
        mockMvc.perform(get("/genres/khong-co-the-loai-nay")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(RankingType.class)
    void rankings_renderForEveryCriterion(RankingType type) throws Exception {
        mockMvc.perform(get("/rankings").param("type", type.name()))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STORY_RANKINGS));
    }

    @Test
    void storyDetail_showsPublishedChaptersOnly() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.PUBLISHED);
        createChapter(story, 2, ChapterStatus.DRAFT);
        createChapter(story, 3, ChapterStatus.SCHEDULED);
        createChapter(story, 4, ChapterStatus.HIDDEN);

        mockMvc.perform(get("/stories/{slug}", story.getSlug()))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STORY_DETAIL))
                .andExpect(content().string(containsString("Tên chương 1")))
                .andExpect(content().string(not(containsString("Tên chương 2"))))
                .andExpect(content().string(not(containsString("Tên chương 3"))))
                .andExpect(content().string(not(containsString("Tên chương 4"))));
    }

    @ParameterizedTest
    @EnumSource(value = StoryVisibility.class, names = {"DRAFT", "HIDDEN"})
    void nonPublicStory_is404ForStrangers_butPreviewableByAuthorAndAdmin(StoryVisibility visibility) throws Exception {
        Story story = createStory(StoryType.NOVEL, created -> created.setVisibility(visibility));
        createChapter(story, 1, ChapterStatus.PUBLISHED);
        String storyUrl = "/stories/" + story.getSlug();
        String chapterUrl = storyUrl + "/chapters/1";

        // 404 chứ không phải 403: người lạ không được biết là có một truyện chưa công khai ở địa chỉ này
        mockMvc.perform(get(storyUrl)).andExpect(status().isNotFound());
        mockMvc.perform(get(chapterUrl)).andExpect(status().isNotFound());
        mockMvc.perform(get(storyUrl).with(user(principalOf(Role.AUTHOR)))).andExpect(status().isNotFound());

        mockMvc.perform(get(storyUrl).with(user(principalOf(story.getAuthor()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("xem trước")));
        mockMvc.perform(get(chapterUrl).with(user(principalOf(Role.ADMIN)))).andExpect(status().isOk());
    }

    @Test
    void novelChapter_rendersStoredHtml_withLinksToNeighbouringPublishedChapters() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.PUBLISHED);
        createChapter(story, 2, ChapterStatus.DRAFT);
        createChapter(story, 3, ChapterStatus.PUBLISHED);
        createChapter(story, 5, ChapterStatus.PUBLISHED);
        String base = "/stories/" + story.getSlug() + "/chapters/";

        // Chương 2 là bản nháp nên "chương trước" của chương 3 là chương 1; số chương không cần liên tiếp
        mockMvc.perform(get(base + 3))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.CHAPTER_READ))
                .andExpect(content().string(containsString("<p>Nội dung chương 3 của " + story.getSlug() + "</p>")))
                .andExpect(content().string(containsString("href=\"" + base + "1\"")))
                .andExpect(content().string(containsString("href=\"" + base + "5\"")))
                .andExpect(content().string(not(containsString("href=\"" + base + "2\""))));
    }

    @Test
    void comicChapter_rendersPagesInOrderWithDimensions() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Long chapterId = createChapter(story, 1, ChapterStatus.PUBLISHED).getId();

        mockMvc.perform(get("/stories/{slug}/chapters/1", story.getSlug()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/media/stories/test/" + chapterId + "-1.png")))
                .andExpect(content().string(containsString("width=\"800\"")))
                .andExpect(content().string(containsString("height=\"1200\"")));
    }

    @ParameterizedTest
    @EnumSource(value = ChapterStatus.class, names = {"DRAFT", "SCHEDULED", "HIDDEN"})
    void unpublishedChapter_is404ForReaders_butPreviewableByAuthor(ChapterStatus status) throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, status);
        String chapterUrl = "/stories/" + story.getSlug() + "/chapters/1";

        mockMvc.perform(get(chapterUrl)).andExpect(status().isNotFound());
        mockMvc.perform(get(chapterUrl).with(user(principalOf(Role.USER)))).andExpect(status().isNotFound());
        mockMvc.perform(get(chapterUrl).with(user(principalOf(story.getAuthor()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("xem trước")));
    }

    @Test
    void missingStoryOrChapter_is404() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(get("/stories/khong-co-truyen-nay")).andExpect(status().isNotFound());
        mockMvc.perform(get("/stories/{slug}/chapters/99", story.getSlug())).andExpect(status().isNotFound());
    }

    @Test
    void libraryAndHistory_requireLogin() throws Exception {
        mockMvc.perform(get("/me/library")).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/me/history")).andExpect(redirectedUrlPattern("**/login"));
    }
}
