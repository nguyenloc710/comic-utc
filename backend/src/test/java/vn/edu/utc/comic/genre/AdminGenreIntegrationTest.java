package vn.edu.utc.comic.genre;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.genre.repository.GenreRepository;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.enums.Role;

class AdminGenreIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private StoryRepository storyRepository;

    private AppUserPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = principalOf(Role.ADMIN);
    }

    @Test
    void genreAdministration_isForAdminsOnly() throws Exception {
        mockMvc.perform(get("/admin/genres").with(user(principalOf(Role.AUTHOR))))
                .andExpect(status().isForbidden());
        mockMvc.perform(saveRequest("/admin/genres", "Không Được Tạo " + uniqueSuffix())
                        .with(user(principalOf(Role.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void genreList_showsSeededGenres() throws Exception {
        mockMvc.perform(get("/admin/genres").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_GENRES))
                .andExpect(content().string(containsString("tu-tien")));
    }

    @Test
    void create_generatesSlugFromVietnameseName() throws Exception {
        String suffix = uniqueSuffix();

        mockMvc.perform(saveRequest("/admin/genres", "  Đặc Vụ Ngầm " + suffix + "  ").with(user(admin)))
                .andExpect(redirectedUrl("/admin/genres"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        Genre created = findBySlug("dac-vu-ngam-" + suffix);
        assertThat(created.getName()).isEqualTo("Đặc Vụ Ngầm " + suffix);
        assertThat(created.getSortOrder()).isEqualTo(7);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void create_rejectsNameThatDuplicatesOrSlugifiesToExistingGenre() throws Exception {
        // "Tu tiên" có sẵn từ dữ liệu khởi tạo; "TU-TIEN" khác tên nhưng sinh ra cùng slug
        mockMvc.perform(saveRequest("/admin/genres", "tu tiên").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_GENRE_FORM))
                .andExpect(model().attributeHasFieldErrorCode(ViewConstants.ATTR_FORM, "name",
                        "error.genre.name.duplicated"));
        mockMvc.perform(saveRequest("/admin/genres", "TU-TIEN").with(user(admin)))
                .andExpect(model().attributeHasFieldErrorCode(ViewConstants.ATTR_FORM, "name",
                        "error.genre.name.duplicated"));
    }

    @Test
    void create_rejectsNameWithoutAnyLetterOrDigit() throws Exception {
        mockMvc.perform(saveRequest("/admin/genres", "!!! ???").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode(ViewConstants.ATTR_FORM, "name",
                        "error.genre.name.invalid"));
    }

    @Test
    void update_changesFields_butKeepsSlug() throws Exception {
        String suffix = uniqueSuffix();
        Genre genre = createGenre("Tên Cũ " + suffix);

        mockMvc.perform(post("/admin/genres/{id}", genre.getId()).with(csrf()).with(user(admin))
                        .param("name", "Tên Mới " + suffix)
                        .param("description", "Mô tả mới")
                        .param("sortOrder", "42")
                        // Dấu hiệu Thymeleaf gửi kèm mỗi checkbox: có nó mà thiếu "active" nghĩa là ô không được tích
                        .param("_active", "on"))
                .andExpect(redirectedUrl("/admin/genres"));

        Genre updated = genreRepository.findById(genre.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Tên Mới " + suffix);
        assertThat(updated.getSlug()).isEqualTo(genre.getSlug());
        assertThat(updated.getDescription()).isEqualTo("Mô tả mới");
        assertThat(updated.getSortOrder()).isEqualTo(42);
        // Ô "đang dùng" không được tích nên thể loại bị tắt
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void update_allowsKeepingOwnName_butRejectsAnotherGenresName() throws Exception {
        Genre genre = createGenre("Giữ Tên " + uniqueSuffix());

        mockMvc.perform(saveRequest("/admin/genres/" + genre.getId(), genre.getName()).with(user(admin)))
                .andExpect(redirectedUrl("/admin/genres"));
        mockMvc.perform(saveRequest("/admin/genres/" + genre.getId(), "Tu tiên").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors(ViewConstants.ATTR_FORM, "name"));
    }

    @Test
    void editForm_isPrefilled_andUnknownGenreGives404() throws Exception {
        Genre genre = createGenre("Xem Form " + uniqueSuffix());

        mockMvc.perform(get("/admin/genres/{id}/edit", genre.getId()).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(genre.getName())));
        mockMvc.perform(get("/admin/genres/{id}/edit", Long.MAX_VALUE).with(user(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_removesUnusedGenre() throws Exception {
        Genre genre = createGenre("Sẽ Xóa " + uniqueSuffix());

        mockMvc.perform(post("/admin/genres/{id}/delete", genre.getId()).with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin/genres"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_SUCCESS));

        assertThat(genreRepository.existsById(genre.getId())).isFalse();
    }

    @Test
    void delete_keepsGenreThatStoriesStillUse() throws Exception {
        Genre genre = createGenre("Đang Dùng " + uniqueSuffix());
        attachToNewStory(genre);

        mockMvc.perform(post("/admin/genres/{id}/delete", genre.getId()).with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin/genres"))
                .andExpect(flash().attributeExists(ViewConstants.ATTR_FLASH_ERROR));

        assertThat(genreRepository.existsById(genre.getId())).isTrue();
    }

    private static MockHttpServletRequestBuilder saveRequest(String url, String name) {
        return post(url).with(csrf())
                .param("name", name)
                .param("description", "Mô tả để thử")
                .param("sortOrder", "7")
                .param("active", "true");
    }

    private Genre createGenre(String name) throws Exception {
        mockMvc.perform(saveRequest("/admin/genres", name).with(user(admin)))
                .andExpect(redirectedUrl("/admin/genres"));
        return genreRepository.findAll().stream()
                .filter(genre -> genre.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private Genre findBySlug(String slug) {
        return genreRepository.findAll().stream()
                .filter(genre -> genre.getSlug().equals(slug))
                .findFirst().orElseThrow();
    }

    private void attachToNewStory(Genre genre) {
        Story story = new Story();
        story.setSlug("truyen-thu-the-loai-" + uniqueSuffix());
        story.setTitle("Truyện thử thể loại");
        story.setDescription("Mô tả");
        story.setType(StoryType.COMIC);
        story.setAuthor(createAccount(Role.AUTHOR));
        story.getGenres().add(genre);
        storyRepository.saveAndFlush(story);
    }
}
