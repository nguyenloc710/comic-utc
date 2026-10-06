package vn.edu.utc.comic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.author.entity.AuthorRequest;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;
import vn.edu.utc.comic.author.enums.IntendedStoryType;
import vn.edu.utc.comic.author.repository.AuthorRequestRepository;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.entity.ChapterContent;
import vn.edu.utc.comic.chapter.repository.ChapterContentRepository;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.genre.repository.GenreRepository;
import vn.edu.utc.comic.interaction.entity.StoryFollow;
import vn.edu.utc.comic.interaction.entity.StoryRating;
import vn.edu.utc.comic.interaction.entity.UserStoryId;
import vn.edu.utc.comic.interaction.repository.StoryFollowRepository;
import vn.edu.utc.comic.interaction.repository.StoryRatingRepository;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Kiểm chứng lược đồ và ánh xạ JPA trên MySQL thật: context khởi động được nghĩa là Flyway chạy sạch và
 * Hibernate validate khớp; các test dưới đây đi xa hơn validate bằng cách ghi/đọc thật.
 */
@Transactional
class SchemaIntegrationTest extends AbstractIntegrationTest {

    private static final int SEEDED_IMAGE_MAX_SIZE_MB = 5;
    private static final int MINIMUM_SEEDED_GENRES = 30;

    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private SettingService settingService;
    @Autowired
    private AuthorRequestRepository authorRequestRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private ChapterContentRepository chapterContentRepository;
    @Autowired
    private StoryFollowRepository storyFollowRepository;
    @Autowired
    private StoryRatingRepository storyRatingRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void seedData_containsAdminGenresAndSettings() {
        assertThat(userAccountRepository.findByUsernameOrEmail("admin"))
                .hasValueSatisfying(admin -> assertThat(admin.getRole()).isEqualTo(Role.ADMIN));
        assertThat(userAccountRepository.findByUsernameOrEmail("admin@comic.local")).isPresent();
        assertThat(genreRepository.count()).isGreaterThanOrEqualTo(MINIMUM_SEEDED_GENRES);
        assertThat(settingService.getInt(SettingKeys.UPLOAD_IMAGE_MAX_SIZE_MB, -1)).isEqualTo(SEEDED_IMAGE_MAX_SIZE_MB);
    }

    @Test
    void authorRequest_rejectsSecondPendingRequestOfSameUser() {
        UserAccount reader = saveUser("reader-pending");
        authorRequestRepository.saveAndFlush(newPendingRequest(reader, "Bút Danh Một"));

        AuthorRequest duplicate = newPendingRequest(reader, "Bút Danh Hai");

        assertThatThrownBy(() -> authorRequestRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void authorRequest_allowsNewPendingRequestAfterRejection() {
        UserAccount reader = saveUser("reader-retry");
        AuthorRequest rejected = newPendingRequest(reader, "Bút Danh Cũ");
        rejected.setStatus(AuthorRequestStatus.REJECTED);
        rejected.setRejectReason("Giới thiệu quá sơ sài");
        authorRequestRepository.saveAndFlush(rejected);

        AuthorRequest retry = authorRequestRepository.saveAndFlush(newPendingRequest(reader, "Bút Danh Mới"));

        assertThat(retry.getId()).isNotNull();
    }

    @Test
    void coreAggregates_persistAndReloadWithAuditColumns() {
        UserAccount author = saveUser("author-mapping");
        Story story = saveStory(author);
        Chapter chapter = saveChapter(story);
        UserStoryId followId = new UserStoryId(author.getId(), story.getId());
        saveFollowAndRating(followId);
        entityManager.flush();
        entityManager.clear();

        Story reloaded = storyRepository.findById(story.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getGenres()).hasSize(1);
        assertThat(chapterContentRepository.findById(chapter.getId()))
                .hasValueSatisfying(content -> assertThat(content.getContent()).contains("Nội dung"));
        assertThat(storyFollowRepository.findById(followId))
                .hasValueSatisfying(follow -> assertThat(follow.getCreatedAt()).isNotNull());
        assertThat(storyRatingRepository.findById(followId))
                .hasValueSatisfying(rating -> assertThat(rating.getStars()).isEqualTo(4));
    }

    /**
     * Bộ đếm của truyện khai báo updatable = false: câu UPDATE cộng dồn vẫn đổi được, còn việc lưu thực thể
     * (ví dụ sửa tên truyện) không được ghi đè bộ đếm bằng giá trị cũ đang nằm trong bộ nhớ.
     */
    @Test
    void storyCounters_changeThroughAtomicUpdate_andSurviveEntitySave() {
        Story story = saveStory(saveUser("author-counter"));

        int updatedRows = entityManager
                .createQuery("UPDATE Story s SET s.viewCount = s.viewCount + 1 WHERE s.id = :id")
                .setParameter("id", story.getId())
                .executeUpdate();
        entityManager.clear();

        Story stale = storyRepository.findById(story.getId()).orElseThrow();
        stale.setViewCount(999);
        stale.setTitle("Tên truyện mới");
        storyRepository.saveAndFlush(stale);
        entityManager.clear();

        Story reloaded = storyRepository.findById(story.getId()).orElseThrow();
        assertThat(updatedRows).isEqualTo(1);
        assertThat(reloaded.getTitle()).isEqualTo("Tên truyện mới");
        assertThat(reloaded.getViewCount()).isEqualTo(1);
    }

    private UserAccount saveUser(String username) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash("unused");
        user.setDisplayName(username);
        return userAccountRepository.saveAndFlush(user);
    }

    private static AuthorRequest newPendingRequest(UserAccount user, String penName) {
        AuthorRequest request = new AuthorRequest();
        request.setUser(user);
        request.setPenName(penName);
        request.setIntroduction("Tôi viết truyện đã ba năm.");
        request.setIntendedType(IntendedStoryType.NOVEL);
        return request;
    }

    private Story saveStory(UserAccount author) {
        Story story = new Story();
        story.setSlug("truyen-thu-anh-xa");
        story.setTitle("Truyện thử ánh xạ");
        story.setDescription("Mô tả dài của truyện.");
        story.setType(StoryType.NOVEL);
        story.setAuthor(author);
        story.getGenres().add(genreRepository.findAll().getFirst());
        return storyRepository.saveAndFlush(story);
    }

    private Chapter saveChapter(Story story) {
        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setChapterNo(1);
        chapter.setTitle("Mở đầu");
        chapterRepository.saveAndFlush(chapter);

        ChapterContent content = new ChapterContent();
        content.setChapterId(chapter.getId());
        content.setContent("<p>Nội dung chương một.</p>");
        chapterContentRepository.saveAndFlush(content);
        return chapter;
    }

    private void saveFollowAndRating(UserStoryId id) {
        StoryFollow follow = new StoryFollow();
        follow.setId(id);
        storyFollowRepository.saveAndFlush(follow);

        StoryRating rating = new StoryRating();
        rating.setId(id);
        rating.setStars(4);
        rating.setUpdatedAt(Instant.now());
        storyRatingRepository.saveAndFlush(rating);
    }
}
