package vn.edu.utc.comic.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Quản lý truyện trong khu vực tác giả: tạo, sửa, công khai, xóa, và quy tắc "chỉ chạm được vào truyện của mình".
 */
class StudioStoryIntegrationTest extends AbstractIntegrationTest {

    private static final String DESCRIPTION = "Một kiếm khách trẻ lên đường tìm lại sư môn đã thất lạc.";

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"USER", "ADMIN"})
    void studio_isForAuthorsOnly_notEvenAdmins(Role role) throws Exception {
        mockMvc.perform(get("/studio/stories").with(user(principalOf(role)))).andExpect(status().isForbidden());
        mockMvc.perform(get("/studio/stories/new").with(user(principalOf(role)))).andExpect(status().isForbidden());
    }

    @Test
    void create_makesADraftWithSlugGenresAndCover_thatReadersCannotSeeYet() throws Exception {
        UserAccount author = createAccount(Role.AUTHOR);
        String suffix = uniqueSuffix();

        mockMvc.perform(storyRequest("/studio/stories", author, "  Kiếm Đạo Độc Tôn " + suffix + "  ", "NOVEL")
                        .file(cover())
                        .param("altTitle", "  ")
                        .param("genreIds", genreId("tu-tien"), genreId("nu-cuong")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Story story = storyRepository.findBySlug("kiem-dao-doc-ton-" + suffix).orElseThrow();
        assertThat(story.getTitle()).isEqualTo("Kiếm Đạo Độc Tôn " + suffix);
        assertThat(story.getAltTitle()).isNull();
        assertThat(story.getVisibility()).isEqualTo(StoryVisibility.DRAFT);
        assertThat(story.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(story.getCoverPath()).startsWith("stories/" + story.getId() + "/cover/").endsWith(".png");
        assertThat(genreSlugsOf(story)).containsExactlyInAnyOrder("tu-tien", "nu-cuong");
        mockMvc.perform(get("/stories/{slug}", story.getSlug())).andExpect(status().isNotFound());
        mockMvc.perform(get("/studio/stories").with(user(principalOf(author))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_STORIES))
                .andExpect(content().string(containsString(story.getTitle())));
    }

    @Test
    void create_withATitleAlreadyUsed_getsANumberedSlug() throws Exception {
        UserAccount author = createAccount(Role.AUTHOR);
        String title = "Trùng Tên " + uniqueSuffix();
        mockMvc.perform(storyRequest("/studio/stories", author, title, "COMIC"));

        mockMvc.perform(storyRequest("/studio/stories", author, title, "COMIC"));

        String baseSlug = "trung-ten-" + title.substring(title.lastIndexOf(' ') + 1);
        assertThat(storyRepository.findBySlug(baseSlug)).isPresent();
        assertThat(storyRepository.findBySlug(baseSlug + "-2")).isPresent();
    }

    @Test
    void create_showsFieldErrors_andCreatesNothing() throws Exception {
        UserAccount author = createAccount(Role.AUTHOR);

        mockMvc.perform(multipart("/studio/stories").with(csrf()).with(user(principalOf(author)))
                        .param("title", " ")
                        .param("description", "")
                        .param("status", "ONGOING"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.STUDIO_STORY_FORM))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "title", "description", "type"));
        // Tệp mang đuôi .png nhưng nội dung không phải ảnh: lỗi hiện ở ô chọn bìa, truyện không được tạo
        mockMvc.perform(storyRequest("/studio/stories", author, "Bìa Hỏng " + uniqueSuffix(), "NOVEL")
                        .file(new MockMultipartFile("cover", "bia.png", "image/png", "không phải ảnh".getBytes())))
                .andExpect(view().name(ViewConstants.STUDIO_STORY_FORM))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "cover"));
        // Tên chỉ gồm ký hiệu thì không sinh được địa chỉ trang truyện
        mockMvc.perform(storyRequest("/studio/stories", author, "!!! ???", "NOVEL"))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "title"));
        // Id thể loại không tồn tại nghĩa là request bị sửa tay
        mockMvc.perform(storyRequest("/studio/stories", author, "Thể Loại Lạ " + uniqueSuffix(), "NOVEL")
                        .param("genreIds", String.valueOf(Long.MAX_VALUE)))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "genreIds"));

        assertThat(countStoriesOf(author)).isZero();
    }

    @Test
    void author_cannotTouchAnotherAuthorsStory() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        UserAccount intruder = createAccount(Role.AUTHOR);

        // 404 chứ không phải 403: tác giả khác không được biết truyện này có trong studio của ai
        mockMvc.perform(get("/studio/stories/{id}/edit", story.getId()).with(user(principalOf(intruder))))
                .andExpect(status().isNotFound());
        mockMvc.perform(storyRequest("/studio/stories/" + story.getId(), intruder, "Chiếm Truyện", "NOVEL"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/studio/stories/{id}/delete", story.getId()).with(csrf()).with(user(principalOf(intruder))))
                .andExpect(status().isNotFound());

        Story unchanged = storyRepository.findById(story.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo(story.getTitle());
        assertThat(unchanged.getDeletedAt()).isNull();
    }

    @Test
    void update_changesFields_butKeepsSlugAndCover() throws Exception {
        Story story = createStory(StoryType.NOVEL, created -> {
            created.setCoverPath("stories/test/cover/bia-cu.png");
            addGenres(created, "tu-tien");
        });
        String newTitle = "Tên Mới " + uniqueSuffix();

        mockMvc.perform(get("/studio/stories/{id}/edit", story.getId()).with(user(principalOf(story.getAuthor()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(story.getTitle())));
        mockMvc.perform(storyRequest("/studio/stories/" + story.getId(), story.getAuthor(), newTitle, "COMIC")
                        .param("status", "COMPLETED")
                        .param("genreIds", genreId("trinh-tham")))
                .andExpect(redirectedUrl("/studio/stories"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Story updated = storyRepository.findById(story.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo(newTitle);
        assertThat(updated.getSlug()).isEqualTo(story.getSlug());
        assertThat(updated.getCoverPath()).isEqualTo("stories/test/cover/bia-cu.png");
        // Truyện chưa có chương nên còn đổi được loại
        assertThat(updated.getType()).isEqualTo(StoryType.COMIC);
        assertThat(updated.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(genreSlugsOf(updated)).containsExactly("trinh-tham");
    }

    @Test
    void update_cannotChangeType_onceTheStoryHasAChapter() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        createChapter(story, 1, ChapterStatus.DRAFT);

        mockMvc.perform(storyRequest("/studio/stories/" + story.getId(), story.getAuthor(), story.getTitle(), "COMIC"))
                .andExpect(view().name(ViewConstants.STUDIO_STORY_FORM))
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "type"));

        assertThat(storyRepository.findById(story.getId()).orElseThrow().getType()).isEqualTo(StoryType.NOVEL);
    }

    @Test
    void publish_needsCoverAndGenre_thenMakesTheStoryPublic() throws Exception {
        String marker = "congkhai" + uniqueSuffix();
        Story story = createStory(StoryType.NOVEL, created -> {
            created.setTitle("Sắp Công Khai " + marker);
            created.setVisibility(StoryVisibility.DRAFT);
            created.setPublishedAt(null);
        });

        mockMvc.perform(publish(story)).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));
        assertThat(visibilityOf(story)).isEqualTo(StoryVisibility.DRAFT);

        mockMvc.perform(storyRequest("/studio/stories/" + story.getId(), story.getAuthor(), story.getTitle(), "NOVEL")
                .file(cover())
                .param("genreIds", genreId("tu-tien")));
        mockMvc.perform(publish(story))
                .andExpect(redirectedUrl("/studio/stories"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Story published = storyRepository.findById(story.getId()).orElseThrow();
        assertThat(published.getVisibility()).isEqualTo(StoryVisibility.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
        mockMvc.perform(get("/stories").param("keyword", marker))
                .andExpect(content().string(containsString(story.getTitle())));
    }

    @Test
    void storyHiddenByAdmin_cannotBeRepublishedByItsAuthor_whoSeesTheReason() throws Exception {
        String reason = "Vi phạm bản quyền " + uniqueSuffix();
        Story story = createStory(StoryType.NOVEL, created -> {
            created.setVisibility(StoryVisibility.HIDDEN);
            created.setHiddenReason(reason);
        });

        mockMvc.perform(publish(story)).andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(visibilityOf(story)).isEqualTo(StoryVisibility.HIDDEN);
        mockMvc.perform(get("/studio/stories").with(user(principalOf(story.getAuthor()))))
                .andExpect(content().string(containsString(reason)));
    }

    @Test
    void delete_isSoft_andHidesTheStoryFromEveryone() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);

        mockMvc.perform(post("/studio/stories/{id}/delete", story.getId()).with(csrf())
                        .with(user(principalOf(story.getAuthor()))))
                .andExpect(redirectedUrl("/studio/stories"));

        assertThat(storyRepository.findById(story.getId()).orElseThrow().getDeletedAt()).isNotNull();
        mockMvc.perform(get("/stories/{slug}", story.getSlug())).andExpect(status().isNotFound());
        mockMvc.perform(get("/studio/stories").with(user(principalOf(story.getAuthor()))))
                .andExpect(content().string(not(containsString(story.getTitle()))));
        // Đã xóa thì chính tác giả cũng không mở lại form sửa được
        mockMvc.perform(get("/studio/stories/{id}/edit", story.getId()).with(user(principalOf(story.getAuthor()))))
                .andExpect(status().isNotFound());
    }

    private MockMultipartHttpServletRequestBuilder storyRequest(String url, UserAccount author, String title,
                                                                String type) {
        MockMultipartHttpServletRequestBuilder request = multipart(url);
        request.with(csrf()).with(user(principalOf(author)));
        request.param("title", title).param("description", DESCRIPTION).param("type", type);
        return request;
    }

    private RequestBuilder publish(Story story) {
        return post("/studio/stories/{id}/publish", story.getId()).with(csrf())
                .with(user(principalOf(story.getAuthor())));
    }

    private static MockMultipartFile cover() {
        return new MockMultipartFile("cover", "bia.png", "image/png", pngBytes(300, 400));
    }

    private String genreId(String slug) {
        return String.valueOf(jdbcTemplate.queryForObject("SELECT id FROM genre WHERE slug = ?", Long.class, slug));
    }

    private List<String> genreSlugsOf(Story story) {
        return jdbcTemplate.queryForList(
                "SELECT g.slug FROM story_genre sg JOIN genre g ON g.id = sg.genre_id WHERE sg.story_id = ?",
                String.class, story.getId());
    }

    private StoryVisibility visibilityOf(Story story) {
        return storyRepository.findById(story.getId()).orElseThrow().getVisibility();
    }

    private long countStoriesOf(UserAccount author) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM story WHERE author_id = ?", Long.class, author.getId());
    }
}
