package vn.edu.utc.comic.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.utc.comic.chapter.dto.StudioChapterPageResponse;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.chapter.service.ChapterPageService;
import vn.edu.utc.comic.chapter.service.ChapterPublishService;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * API trang ảnh của chương truyện tranh: tải lên từng ảnh, sắp xếp, xóa — số trang luôn liền mạch 1..n.
 */
class ChapterPageApiIntegrationTest extends AbstractIntegrationTest {

    private static final String MAX_PAGES_KEY = "upload.chapter.max_pages";
    /** Nhỏ hơn số kết nối của pool để mọi luồng cùng vào được cơ sở dữ liệu một lúc. */
    private static final int CONCURRENT_UPLOADS = 6;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SettingService settingService;

    @Autowired
    private ChapterPublishService chapterPublishService;

    @Autowired
    private ChapterPageService chapterPageService;

    @Test
    void upload_appendsPagesInOrder_withRealDimensions_andLetsTheChapterBePublished() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createEmptyChapter(story, ChapterStatus.DRAFT);
        UserAccount author = story.getAuthor();

        upload(chapter, author, png("1.png", 600, 900))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.width").value(600))
                .andExpect(jsonPath("$.data.height").value(900))
                .andExpect(jsonPath("$.data.imageUrl").value(
                        startsWith("/media/stories/" + story.getId() + "/chapters/" + chapter.getId() + "/")));
        upload(chapter, author, png("2.png", 400, 400)).andExpect(jsonPath("$.data.pageNo").value(2));

        assertThat(pageCountOf(chapter)).isEqualTo(2);
        mockMvc.perform(get(pagesUrl(chapter)).with(user(principalOf(author))))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].width").value(600))
                .andExpect(jsonPath("$.data[1].width").value(400));
        // Có ảnh rồi thì chương đăng được
        chapterPublishService.publishNow(chapter.getId(), author.getId());
        assertThat(storyCounter(story, "chapter_count")).isEqualTo(1);
    }

    @Test
    void concurrentUploads_toTheSameChapter_neverShareAPageNumber() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createEmptyChapter(story, ChapterStatus.DRAFT);
        Long authorId = story.getAuthor().getId();
        CountDownLatch startTogether = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_UPLOADS)) {
            List<Future<StudioChapterPageResponse>> uploads = IntStream.range(0, CONCURRENT_UPLOADS)
                    .mapToObj(index -> executor.submit(() -> {
                        startTogether.await();
                        return chapterPageService.addPage(chapter.getId(), authorId, png(index + ".png", 50, 50));
                    }))
                    .toList();
            startTogether.countDown();
            for (Future<StudioChapterPageResponse> upload : uploads) {
                upload.get(15, TimeUnit.SECONDS);
            }
        }

        // Mỗi lượt tải phải thấy các trang mà lượt trước nó vừa thêm: số trang liền mạch 1..n, không trùng
        assertThat(jdbcTemplate.queryForList("SELECT page_no FROM chapter_page WHERE chapter_id = ? ORDER BY page_no",
                Integer.class, chapter.getId())).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(pageCountOf(chapter)).isEqualTo(CONCURRENT_UPLOADS);
    }

    @Test
    void upload_rejectsFilesThatAreNotImages_whateverTheirNameSays() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createEmptyChapter(story, ChapterStatus.DRAFT);

        upload(chapter, story.getAuthor(), new MockMultipartFile("file", "trang.png", "image/png", "<?php ?>".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IMAGE_TYPE_NOT_ALLOWED"));

        assertThat(pageCountOf(chapter)).isZero();
        assertThat(countPageRows(chapter)).isZero();
    }

    @Test
    void upload_stopsAtTheConfiguredPageLimit() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createEmptyChapter(story, ChapterStatus.DRAFT);
        String originalLimit = jdbcTemplate.queryForObject("SELECT setting_value FROM setting WHERE setting_key = ?",
                String.class, MAX_PAGES_KEY);
        setMaxPages("1");
        try {
            upload(chapter, story.getAuthor(), png("1.png", 100, 100)).andExpect(status().isCreated());

            upload(chapter, story.getAuthor(), png("2.png", 100, 100))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("CHAPTER_PAGE_LIMIT"));
        } finally {
            // Bảng setting dùng chung cho mọi test: phải trả lại giá trị cũ dù test đạt hay hỏng
            setMaxPages(originalLimit);
        }

        assertThat(pageCountOf(chapter)).isEqualTo(1);
    }

    @Test
    void reorder_renumbersPages_andRefusesListsThatDoNotMatchTheChapter() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createEmptyChapter(story, ChapterStatus.DRAFT);
        Chapter otherChapter = createChapter(story, 9, ChapterStatus.DRAFT);
        UserAccount author = story.getAuthor();
        long first = pageId(upload(chapter, author, png("1.png", 100, 100)));
        long second = pageId(upload(chapter, author, png("2.png", 200, 200)));
        long third = pageId(upload(chapter, author, png("3.png", 300, 300)));
        long foreign = pageIds(otherChapter).getFirst();

        reorder(chapter, author, List.of(third, first, second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(third))
                .andExpect(jsonPath("$.data[0].pageNo").value(1))
                .andExpect(jsonPath("$.data[2].id").value(second))
                .andExpect(jsonPath("$.data[2].pageNo").value(3));
        assertThat(pageIds(chapter)).containsExactly(third, first, second);

        // Thiếu trang, lặp trang, hoặc trang của chương khác đều bị từ chối và thứ tự giữ nguyên
        for (List<Long> badOrder : List.of(List.of(first, second), List.of(first, first, second),
                List.of(first, second, foreign))) {
            reorder(chapter, author, badOrder)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("CHAPTER_PAGE_ORDER_INVALID"));
        }
        reorder(chapter, author, List.of()).andExpect(status().isBadRequest());
        assertThat(pageIds(chapter)).containsExactly(third, first, second);
    }

    @Test
    void delete_closesTheGapInPageNumbers() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createEmptyChapter(story, ChapterStatus.DRAFT);
        UserAccount author = story.getAuthor();
        long first = pageId(upload(chapter, author, png("1.png", 100, 100)));
        long second = pageId(upload(chapter, author, png("2.png", 100, 100)));
        long third = pageId(upload(chapter, author, png("3.png", 100, 100)));

        mockMvc.perform(delete(pagesUrl(chapter) + "/" + second).with(csrf()).with(user(principalOf(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].id").value(third))
                .andExpect(jsonPath("$.data[1].pageNo").value(2));

        assertThat(pageIds(chapter)).containsExactly(first, third);
        assertThat(pageCountOf(chapter)).isEqualTo(2);
        mockMvc.perform(delete(pagesUrl(chapter) + "/" + second).with(csrf()).with(user(principalOf(author))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAPTER_PAGE_NOT_FOUND"));
    }

    @Test
    void lastPage_ofAPublishedChapter_cannotBeDeleted_butADraftMayBecomeEmpty() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        UserAccount author = story.getAuthor();
        Chapter draft = createEmptyChapter(story, ChapterStatus.DRAFT);
        long draftPage = pageId(upload(draft, author, png("1.png", 100, 100)));
        Chapter published = createEmptyChapter(story, ChapterStatus.PUBLISHED);
        long publishedPage = pageId(upload(published, author, png("1.png", 100, 100)));

        mockMvc.perform(delete(pagesUrl(draft) + "/" + draftPage).with(csrf()).with(user(principalOf(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(delete(pagesUrl(published) + "/" + publishedPage).with(csrf()).with(user(principalOf(author))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CHAPTER_EMPTY"));

        assertThat(pageCountOf(draft)).isZero();
        assertThat(pageCountOf(published)).isEqualTo(1);
    }

    @Test
    void pages_areOffLimitsToOtherAuthors_readers_guests_andNovelChapters() throws Exception {
        Story comic = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createEmptyChapter(comic, ChapterStatus.DRAFT);
        Story novel = createPublishedStory(StoryType.NOVEL);
        Chapter novelChapter = createChapter(novel, 1, ChapterStatus.DRAFT);

        upload(chapter, createAccount(Role.AUTHOR), png("1.png", 100, 100))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAPTER_NOT_FOUND"));
        upload(chapter, createAccount(Role.USER), png("1.png", 100, 100)).andExpect(status().isForbidden());
        upload(chapter, createAccount(Role.ADMIN), png("1.png", 100, 100)).andExpect(status().isForbidden());
        mockMvc.perform(multipart(pagesUrl(chapter)).file(png("1.png", 100, 100)).with(csrf()))
                .andExpect(status().isUnauthorized());
        upload(novelChapter, novel.getAuthor(), png("1.png", 100, 100))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CHAPTER_TYPE_MISMATCH"));

        assertThat(countPageRows(chapter)).isZero();
    }

    /** Chương truyện tranh chưa có ảnh nào (khác {@link #createChapter}, vốn gắn sẵn hai trang giả). */
    private Chapter createEmptyChapter(Story story, ChapterStatus status) {
        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setChapterNo(jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(chapter_no), 0) + 1 FROM chapter WHERE story_id = ?", Integer.class, story.getId()));
        chapter.setStatus(status);
        return chapterRepository.saveAndFlush(chapter);
    }

    private ResultActions upload(Chapter chapter, UserAccount account, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart(pagesUrl(chapter)).file(file).with(csrf()).with(user(principalOf(account))));
    }

    private ResultActions reorder(Chapter chapter, UserAccount author, List<Long> pageIds) throws Exception {
        return mockMvc.perform(put(pagesUrl(chapter) + "/order").with(csrf()).with(user(principalOf(author)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("pageIds", pageIds))));
    }

    private static MockMultipartFile png(String fileName, int width, int height) {
        return new MockMultipartFile("file", fileName, "image/png", pngBytes(width, height));
    }

    private static String pagesUrl(Chapter chapter) {
        return "/api/studio/chapters/" + chapter.getId() + "/pages";
    }

    private long pageId(ResultActions uploaded) throws Exception {
        String json = uploaded.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).path("data").path("id").asLong();
    }

    private List<Long> pageIds(Chapter chapter) {
        return jdbcTemplate.queryForList("SELECT id FROM chapter_page WHERE chapter_id = ? ORDER BY page_no", Long.class,
                chapter.getId());
    }

    private long pageCountOf(Chapter chapter) {
        return jdbcTemplate.queryForObject("SELECT page_count FROM chapter WHERE id = ?", Long.class, chapter.getId());
    }

    private long countPageRows(Chapter chapter) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chapter_page WHERE chapter_id = ?", Long.class,
                chapter.getId());
    }

    private void setMaxPages(String value) {
        jdbcTemplate.update("UPDATE setting SET setting_value = ? WHERE setting_key = ?", value, MAX_PAGES_KEY);
        settingService.clearCache();
    }
}
