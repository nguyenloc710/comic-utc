package vn.edu.utc.comic.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.chapter.job.ChapterPublishJob;
import vn.edu.utc.comic.chapter.service.ChapterPublishService;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Đăng chương hẹn giờ. Điều phải giữ đúng: mỗi chương được đăng ĐÚNG MỘT LẦN dù job chạy lặp — số chương của
 * truyện chỉ cộng một, người theo dõi chỉ nhận một thông báo.
 */
class ChapterPublishJobIntegrationTest extends AbstractIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);
    /** Đủ lâu để một thông báo bị gửi thừa (nếu có) kịp ghi xuống trước khi khẳng định là không có. */
    private static final Duration SETTLE_TIME = Duration.ofMillis(700);

    @Autowired
    private ChapterPublishJob chapterPublishJob;

    @Autowired
    private ChapterPublishService chapterPublishService;

    @Test
    void jobRunThreeTimes_publishesADueChapterExactlyOnce() {
        Story story = createPublishedStory(StoryType.NOVEL);
        Chapter chapter = createDueChapter(story, 1);
        UserAccount firstFollower = follow(story);
        UserAccount secondFollower = follow(story);
        long jobRunsBefore = countJobRuns();

        int firstRun = chapterPublishJob.publishDueChapters();
        int secondRun = chapterPublishJob.publishDueChapters();
        int thirdRun = chapterPublishJob.publishDueChapters();

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isZero();
        assertThat(thirdRun).isZero();
        Chapter published = chapterRepository.findById(chapter.getId()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(ChapterStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.getScheduledAt()).isNull();
        assertThat(storyCounter(story, "chapter_count")).isEqualTo(1);
        // Lượt không có gì để đăng không ghi job_run
        assertThat(countJobRuns()).isEqualTo(jobRunsBefore + 1);
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            assertThat(countNewChapterNotifications(firstFollower)).isEqualTo(1);
            assertThat(countNewChapterNotifications(secondFollower)).isEqualTo(1);
        });
        await().during(SETTLE_TIME).atMost(ASYNC_TIMEOUT).until(() -> countNewChapterNotifications(firstFollower) == 1);
    }

    @Test
    void job_leavesChaptersThatAreNotDueYet() {
        Story story = createPublishedStory(StoryType.NOVEL);
        Chapter chapter = createChapter(story, 1, ChapterStatus.SCHEDULED);
        setScheduledAt(chapter, Instant.now().plus(1, ChronoUnit.DAYS));

        chapterPublishJob.publishDueChapters();

        assertThat(chapterRepository.findById(chapter.getId()).orElseThrow().getStatus())
                .isEqualTo(ChapterStatus.SCHEDULED);
        assertThat(storyCounter(story, "chapter_count")).isZero();
    }

    @Test
    void dueChapter_thatTheAuthorJustUnscheduled_isNotPublished() {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createDueChapter(story, 1);
        chapterPublishService.cancelSchedule(chapter.getId(), story.getAuthor().getId());

        // Job đã lấy id chương này từ trước khi tác giả hủy hẹn: câu UPDATE có điều kiện phải bỏ qua nó
        boolean published = chapterPublishService.publishDueChapter(chapter.getId());

        assertThat(published).isFalse();
        assertThat(chapterRepository.findById(chapter.getId()).orElseThrow().getStatus()).isEqualTo(ChapterStatus.DRAFT);
        assertThat(storyCounter(story, "chapter_count")).isZero();
    }

    @Test
    void chapterOfAStoryThatIsNotPublic_isPublishedWithoutNotifyingAnyone() {
        Story hiddenStory = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.HIDDEN));
        createDueChapter(hiddenStory, 1);
        UserAccount follower = follow(hiddenStory);

        chapterPublishJob.publishDueChapters();

        assertThat(storyCounter(hiddenStory, "chapter_count")).isEqualTo(1);
        await().during(SETTLE_TIME).atMost(ASYNC_TIMEOUT).until(() -> countNewChapterNotifications(follower) == 0);
    }

    @Test
    void publishNow_notifiesFollowersOnce_andCannotBeRepeated() {
        Story story = createStory(StoryType.COMIC,
                created -> created.setLastChapterAt(Instant.now().minus(10, ChronoUnit.DAYS)));
        Chapter chapter = createChapter(story, 3, ChapterStatus.DRAFT);
        UserAccount follower = follow(story);
        Long authorId = story.getAuthor().getId();

        chapterPublishService.publishNow(chapter.getId(), authorId);

        assertThat(storyCounter(story, "chapter_count")).isEqualTo(1);
        // Truyện được đẩy lên đầu danh sách "mới cập nhật"
        assertThat(storyRepository.findById(story.getId()).orElseThrow().getLastChapterAt())
                .isAfter(Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> chapterPublishService.publishNow(chapter.getId(), authorId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAPTER_INVALID_TRANSITION));
        assertThat(storyCounter(story, "chapter_count")).isEqualTo(1);
        await().atMost(ASYNC_TIMEOUT).untilAsserted(
                () -> assertThat(countNewChapterNotifications(follower)).isEqualTo(1));
        assertThat(jdbcTemplate.queryForObject("SELECT link FROM notification WHERE recipient_id = ?", String.class,
                follower.getId())).isEqualTo("/stories/" + story.getSlug() + "/chapters/3");
    }

    @Test
    void emptyChapter_canBeNeitherPublishedNorScheduled() {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createChapter(story, 1, ChapterStatus.DRAFT);
        jdbcTemplate.update("UPDATE chapter SET page_count = 0 WHERE id = ?", chapter.getId());
        Long authorId = story.getAuthor().getId();

        assertThatThrownBy(() -> chapterPublishService.publishNow(chapter.getId(), authorId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAPTER_EMPTY));
        assertThatThrownBy(() -> chapterPublishService.schedule(chapter.getId(), authorId,
                LocalDateTime.of(2099, 1, 1, 8, 0)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAPTER_EMPTY));
    }

    /** Chương đang hẹn giờ với giờ hẹn đã qua một phút. */
    private Chapter createDueChapter(Story story, int chapterNo) {
        Chapter chapter = createChapter(story, chapterNo, ChapterStatus.SCHEDULED);
        setScheduledAt(chapter, Instant.now().minus(1, ChronoUnit.MINUTES));
        return chapter;
    }

    private void setScheduledAt(Chapter chapter, Instant scheduledAt) {
        jdbcTemplate.update("UPDATE chapter SET scheduled_at = ? WHERE id = ?", utc(scheduledAt), chapter.getId());
    }

    private UserAccount follow(Story story) {
        UserAccount follower = createAccount(Role.USER);
        jdbcTemplate.update("INSERT INTO story_follow (user_id, story_id, created_at) VALUES (?, ?, ?)",
                follower.getId(), story.getId(), utc(Instant.now()));
        return follower;
    }

    private long countNewChapterNotifications(UserAccount recipient) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE recipient_id = ? AND type = 'NEW_CHAPTER'", Long.class,
                recipient.getId());
    }

    private long countJobRuns() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job_run WHERE job_name = 'chapter-publish'",
                Long.class);
    }
}
