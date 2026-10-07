package vn.edu.utc.comic.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.notification.enums.NotificationType;
import vn.edu.utc.comic.notification.service.NotificationService;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Thông báo trong ứng dụng: ghi, đếm chưa đọc, mở và đánh dấu đã đọc.
 */
class NotificationIntegrationTest extends AbstractIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private NotificationService notificationService;

    @Test
    void unreadCount_isServedToTheBell_andGuestsGetJson401() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        notificationService.notifyUser(reader.getId(), NotificationType.NEW_CHAPTER, "/stories/a/chapters/1", "A", "1");
        notificationService.notifyUser(reader.getId(), NotificationType.NEW_CHAPTER, "/stories/a/chapters/2", "A", "2");

        mockMvc.perform(get("/api/notifications/unread-count").with(user(principalOf(reader))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_buildsTheSentenceFromTypeAndArguments() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        String storyTitle = "Truyện Có Chương Mới " + uniqueSuffix();
        notificationService.notifyUser(reader.getId(), NotificationType.NEW_CHAPTER, "/stories/x/chapters/7",
                storyTitle, "7");

        mockMvc.perform(get("/me/notifications").with(user(principalOf(reader))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ME_NOTIFICATIONS))
                .andExpect(content().string(containsString(storyTitle)))
                .andExpect(content().string(containsString("vừa có chương 7")));
    }

    @Test
    void opening_marksAsRead_andRedirectsToTheLink() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        notificationService.notifyUser(reader.getId(), NotificationType.NEW_CHAPTER, "/stories/x/chapters/3", "X", "3");
        long notificationId = onlyNotificationId(reader);

        mockMvc.perform(post("/me/notifications/{id}/open", notificationId).with(csrf()).with(user(principalOf(reader))))
                .andExpect(redirectedUrl("/stories/x/chapters/3"));

        assertThat(notificationService.countUnread(reader.getId())).isZero();
    }

    @Test
    void notification_ofSomeoneElse_cannotBeOpened() throws Exception {
        UserAccount owner = createAccount(Role.USER);
        notificationService.notifyUser(owner.getId(), NotificationType.NEW_CHAPTER, "/stories/x", "X", "1");

        mockMvc.perform(post("/me/notifications/{id}/open", onlyNotificationId(owner)).with(csrf())
                        .with(user(principalOf(Role.USER))))
                .andExpect(status().isNotFound());

        assertThat(notificationService.countUnread(owner.getId())).isEqualTo(1);
    }

    @Test
    void opening_neverRedirectsOutsideTheApplication() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        notificationService.notifyUser(reader.getId(), NotificationType.NEW_CHAPTER, "/stories/x", "X", "1");
        long notificationId = onlyNotificationId(reader);

        // Dữ liệu hỏng (đường dẫn trỏ ra ngoài) không được biến trang thông báo thành chỗ chuyển hướng mở
        for (String badLink : new String[] {"https://evil.example/", "//evil.example/"}) {
            jdbcTemplate.update("UPDATE notification SET link = ? WHERE id = ?", badLink, notificationId);
            mockMvc.perform(post("/me/notifications/{id}/open", notificationId).with(csrf())
                            .with(user(principalOf(reader))))
                    .andExpect(redirectedUrl("/me/notifications"));
        }
    }

    @Test
    void readAll_marksOnlyTheUsersOwnNotifications() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        UserAccount other = createAccount(Role.USER);
        notificationService.notifyUser(reader.getId(), NotificationType.NEW_CHAPTER, "/stories/x", "X", "1");
        notificationService.notifyUser(reader.getId(), NotificationType.NEW_CHAPTER, "/stories/x", "X", "2");
        notificationService.notifyUser(other.getId(), NotificationType.NEW_CHAPTER, "/stories/x", "X", "1");

        mockMvc.perform(post("/me/notifications/read-all").with(csrf()).with(user(principalOf(reader))))
                .andExpect(redirectedUrl("/me/notifications"));

        assertThat(notificationService.countUnread(reader.getId())).isZero();
        assertThat(notificationService.countUnread(other.getId())).isEqualTo(1);
    }

    @Test
    void notifyFollowers_writesOneNotificationPerFollower_inOneStatement() {
        Story story = createPublishedStory(StoryType.NOVEL);
        UserAccount firstFollower = follow(story);
        UserAccount secondFollower = follow(story);
        UserAccount stranger = createAccount(Role.USER);

        int recipients = notificationService.notifyFollowers(story.getId(), NotificationType.NEW_CHAPTER,
                "/stories/" + story.getSlug() + "/chapters/1", story.getTitle(), "1");

        assertThat(recipients).isEqualTo(2);
        assertThat(notificationService.countUnread(firstFollower.getId())).isEqualTo(1);
        assertThat(notificationService.countUnread(secondFollower.getId())).isEqualTo(1);
        assertThat(notificationService.countUnread(stranger.getId())).isZero();
    }

    @Test
    void replyToAComment_notifiesItsAuthor_butNotWhenReplyingToOneself() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        UserAccount commenter = createAccount(Role.USER);
        long rootId = insertComment(story, commenter);

        mockMvc.perform(post("/api/comments").with(csrf()).with(user(principalOf(commenter)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyBody(story, rootId, "Tự trả lời mình")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/comments").with(csrf()).with(user(principalOf(Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyBody(story, rootId, "Người khác trả lời")))
                .andExpect(status().isCreated());

        await().atMost(ASYNC_TIMEOUT).untilAsserted(
                () -> assertThat(notificationService.countUnread(commenter.getId())).isEqualTo(1));
        mockMvc.perform(post("/me/notifications/{id}/open", onlyNotificationId(commenter)).with(csrf())
                        .with(user(principalOf(commenter))))
                .andExpect(redirectedUrl("/stories/" + story.getSlug() + "#comments"));
    }

    private UserAccount follow(Story story) {
        UserAccount follower = createAccount(Role.USER);
        jdbcTemplate.update("INSERT INTO story_follow (user_id, story_id, created_at) VALUES (?, ?, ?)",
                follower.getId(), story.getId(), utc(Instant.now()));
        return follower;
    }

    /** Chèn thẳng một bình luận gốc: người viết vừa bình luận qua API sẽ vướng thời gian chờ giữa hai bình luận. */
    private long insertComment(Story story, UserAccount author) {
        jdbcTemplate.update("INSERT INTO comment (story_id, user_id, content, created_at) VALUES (?, ?, ?, ?)",
                story.getId(), author.getId(), "Bình luận gốc", utc(Instant.now().minusSeconds(3600)));
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM comment WHERE story_id = ? AND user_id = ?",
                Long.class, story.getId(), author.getId());
    }

    private static String replyBody(Story story, long parentId, String content) {
        return "{\"storyId\": %d, \"parentId\": %d, \"content\": \"%s\"}".formatted(story.getId(), parentId, content);
    }

    private long onlyNotificationId(UserAccount recipient) {
        return jdbcTemplate.queryForObject("SELECT id FROM notification WHERE recipient_id = ?", Long.class,
                recipient.getId());
    }
}
