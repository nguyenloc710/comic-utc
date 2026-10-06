package vn.edu.utc.comic.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.entity.ChapterContent;
import vn.edu.utc.comic.chapter.entity.ChapterPage;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.chapter.repository.ChapterContentRepository;
import vn.edu.utc.comic.chapter.repository.ChapterPageRepository;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.genre.repository.GenreRepository;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Lớp cha cho test tích hợp: MySQL 8.4 thật do Testcontainers cấp, Flyway chạy đúng migration của ứng dụng.
 *
 * <p>Container được khởi động MỘT lần trong khối static và dùng chung cho mọi lớp test (không dùng
 * {@code @Container}): Spring cache lại ApplicationContext giữa các lớp test, nên nếu mỗi lớp có container
 * riêng thì context đã cache sẽ trỏ vào một cơ sở dữ liệu đã bị tắt. Ryuk dọn container khi JVM kết thúc.
 * Image lấy qua mirror ECR Public vì Docker Hub có thể bị chặn trên mạng nội bộ.
 *
 * <p>Test luồng web KHÔNG chạy trong transaction rollback: nhiều hành vi cần kiểm chứng chỉ xảy ra sau commit
 * (làm mới phiên khi tài khoản đổi, xóa ảnh cũ, đếm lượt xem chạy nền). Vì dữ liệu được commit thật vào cơ sở
 * dữ liệu dùng chung, mỗi test tự tạo tài khoản và truyện với tên ngẫu nhiên qua các hàm {@code create...}
 * thay vì dùng chung dữ liệu, và không khẳng định gì về tổng số dòng của một bảng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /** Mật khẩu của mọi tài khoản do {@link #createAccount} tạo. */
    protected static final String ACCOUNT_PASSWORD = "Test@1234";

    private static final int FAST_BCRYPT_STRENGTH = 4;
    private static final int SUFFIX_LENGTH = 8;
    /** Băm một lần với độ mạnh thấp nhất để test không tốn ~0,3 giây cho mỗi tài khoản. */
    private static final String ACCOUNT_PASSWORD_HASH =
            new BCryptPasswordEncoder(FAST_BCRYPT_STRENGTH).encode(ACCOUNT_PASSWORD);

    @ServiceConnection
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("public.ecr.aws/docker/library/mysql:8.4").asCompatibleSubstituteFor("mysql"))
            // Cùng tham số với docker-compose.yml để hành vi FULLTEXT và múi giờ giống môi trường dev
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci",
                    "--innodb-ft-min-token-size=1", "--default-time-zone=+00:00");

    static {
        MYSQL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected UserAccountRepository userAccountRepository;

    @Autowired
    protected StoryRepository storyRepository;

    @Autowired
    protected ChapterRepository chapterRepository;

    @Autowired
    private ChapterContentRepository chapterContentRepository;

    @Autowired
    private ChapterPageRepository chapterPageRepository;

    @Autowired
    private GenreRepository genreRepository;

    /** Chuỗi ngẫu nhiên ngắn để đặt tên dữ liệu test không đụng nhau giữa các test. */
    protected static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, SUFFIX_LENGTH);
    }

    /** Tạo và commit một tài khoản mới có vai trò cho trước, mật khẩu {@link #ACCOUNT_PASSWORD}. */
    protected UserAccount createAccount(Role role) {
        String username = role.name().toLowerCase() + "_" + uniqueSuffix();
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash(ACCOUNT_PASSWORD_HASH);
        user.setDisplayName("Người thử " + username);
        user.setRole(role);
        return userAccountRepository.saveAndFlush(user);
    }

    /** Principal của một tài khoản mới tạo, dùng với {@code .with(user(...))} khi không cần phiên thật. */
    protected AppUserPrincipal principalOf(Role role) {
        return principalOf(createAccount(role));
    }

    protected static AppUserPrincipal principalOf(UserAccount account) {
        return AppUserPrincipal.from(account, Instant.now());
    }

    /** Đăng nhập thật qua form và trả về phiên, cho các test cần theo dõi một phiên qua nhiều request. */
    protected MockHttpSession login(UserAccount account) throws Exception {
        return (MockHttpSession) mockMvc
                .perform(formLogin("/login").user(account.getUsername()).password(ACCOUNT_PASSWORD))
                .andReturn().getRequest().getSession(false);
    }

    protected UserAccount reload(UserAccount account) {
        return userAccountRepository.findById(account.getId()).orElseThrow();
    }

    /** Truyện đang công khai của một tác giả mới tạo, tên và slug ngẫu nhiên. */
    protected Story createPublishedStory(StoryType type) {
        return createStory(type, story -> { });
    }

    /**
     * Tạo và commit một truyện. Mặc định là truyện đang công khai của một tác giả mới tạo;
     * {@code customizer} sửa các trường cần khác đi (kể cả bộ đếm, vốn chỉ ghi được lúc thêm mới).
     */
    protected Story createStory(StoryType type, Consumer<Story> customizer) {
        String suffix = uniqueSuffix();
        Story story = new Story();
        story.setSlug("truyen-thu-" + suffix);
        story.setTitle("Truyện thử " + suffix);
        story.setDescription("Mô tả của truyện thử " + suffix);
        story.setType(type);
        story.setVisibility(StoryVisibility.PUBLISHED);
        story.setPublishedAt(Instant.now());
        story.setLastChapterAt(Instant.now());
        customizer.accept(story);
        if (story.getAuthor() == null) {
            story.setAuthor(createAccount(Role.AUTHOR));
        }
        return storyRepository.saveAndFlush(story);
    }

    /** Gắn cho truyện (chưa lưu) các thể loại có sẵn từ dữ liệu khởi tạo, theo slug. */
    protected void addGenres(Story story, String... genreSlugs) {
        for (String slug : genreSlugs) {
            story.getGenres().add(genreRepository.findBySlugAndActiveTrue(slug).orElseThrow());
        }
    }

    /**
     * Tạo và commit một chương kèm nội dung: hai trang ảnh với truyện tranh, một đoạn văn với truyện chữ.
     * KHÔNG cập nhật {@code chapter_count} của truyện — test nào cần thì đặt lúc tạo truyện.
     */
    protected Chapter createChapter(Story story, int chapterNo, ChapterStatus status) {
        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setChapterNo(chapterNo);
        chapter.setTitle("Tên chương " + chapterNo);
        chapter.setStatus(status);
        chapter.setPublishedAt(status == ChapterStatus.PUBLISHED ? Instant.now() : null);
        chapterRepository.saveAndFlush(chapter);
        if (story.getType() == StoryType.COMIC) {
            saveChapterPage(chapter, 1);
            saveChapterPage(chapter, 2);
        } else {
            ChapterContent content = new ChapterContent();
            content.setChapterId(chapter.getId());
            content.setContent("<p>Nội dung chương " + chapterNo + " của " + story.getSlug() + "</p>");
            chapterContentRepository.saveAndFlush(content);
        }
        return chapter;
    }

    private void saveChapterPage(Chapter chapter, int pageNo) {
        ChapterPage page = new ChapterPage();
        page.setChapter(chapter);
        page.setPageNo(pageNo);
        page.setImagePath("stories/test/" + chapter.getId() + "-" + pageNo + ".png");
        page.setWidth(800);
        page.setHeight(1200);
        page.setSizeBytes(1024);
        chapterPageRepository.saveAndFlush(page);
    }

    /** Đọc thẳng một bộ đếm của truyện từ cơ sở dữ liệu (thực thể trong bộ nhớ không thấy câu UPDATE cộng dồn). */
    protected long storyCounter(Story story, String column) {
        Long value = jdbcTemplate.queryForObject("SELECT " + column + " FROM story WHERE id = ?", Long.class,
                story.getId());
        return value == null ? 0 : value;
    }
}
