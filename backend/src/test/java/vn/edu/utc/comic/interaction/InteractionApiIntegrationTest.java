package vn.edu.utc.comic.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.interaction.dto.FollowResponse;
import vn.edu.utc.comic.interaction.dto.RatingResponse;
import vn.edu.utc.comic.interaction.service.FollowService;
import vn.edu.utc.comic.interaction.service.RatingService;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Theo dõi và đánh giá: điều cần giữ đúng là bộ đếm của truyện không lệch dù thao tác bị lặp lại.
 */
class InteractionApiIntegrationTest extends AbstractIntegrationTest {

    /** Nhỏ hơn số kết nối của pool để mọi luồng cùng vào được cơ sở dữ liệu một lúc. */
    private static final int CONCURRENT_READERS = 8;

    @Autowired
    private FollowService followService;

    @Autowired
    private RatingService ratingService;

    @Test
    void interactions_requireLogin_andAnswerGuestsWithJson() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(put("/api/stories/{id}/follow", story.getId()).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void follow_andUnfollow_areIdempotent_soFollowCountNeverDrifts() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        AppUserPrincipal reader = principalOf(Role.USER);
        AppUserPrincipal otherReader = principalOf(Role.USER);

        follow(story, reader).andExpect(jsonPath("$.data.following").value(true))
                .andExpect(jsonPath("$.data.followCount").value(1));
        follow(story, reader).andExpect(jsonPath("$.data.followCount").value(1));
        follow(story, otherReader).andExpect(jsonPath("$.data.followCount").value(2));
        assertThat(storyCounter(story, "follow_count")).isEqualTo(2);

        unfollow(story, reader).andExpect(jsonPath("$.data.following").value(false))
                .andExpect(jsonPath("$.data.followCount").value(1));
        unfollow(story, reader).andExpect(jsonPath("$.data.followCount").value(1));
        assertThat(storyCounter(story, "follow_count")).isEqualTo(1);
    }

    @Test
    void concurrentFollows_ofSameStory_allSucceed_andAreAllCounted() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        List<Long> readerIds = IntStream.range(0, CONCURRENT_READERS)
                .mapToObj(index -> createAccount(Role.USER).getId()).toList();
        CountDownLatch startTogether = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_READERS)) {
            List<Future<FollowResponse>> results = readerIds.stream()
                    .map(readerId -> executor.submit(() -> {
                        startTogether.await();
                        return followService.follow(story.getId(), readerId);
                    }))
                    .toList();
            startTogether.countDown();
            // get() ném lại lỗi của luồng con: một deadlock ở cơ sở dữ liệu sẽ làm test hỏng ngay tại đây
            for (Future<FollowResponse> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS).following()).isTrue();
            }
        }

        assertThat(storyCounter(story, "follow_count")).isEqualTo(CONCURRENT_READERS);
    }

    @Test
    void concurrentRatings_bySameUser_leaveExactlyOneRating_andAMatchingSum() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Long readerId = createAccount(Role.USER).getId();
        CountDownLatch startTogether = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_READERS)) {
            List<Future<RatingResponse>> results = IntStream.range(0, CONCURRENT_READERS)
                    .mapToObj(index -> executor.submit(() -> {
                        startTogether.await();
                        return ratingService.rate(story.getId(), readerId, index % 5 + 1);
                    }))
                    .toList();
            startTogether.countDown();
            for (Future<RatingResponse> result : results) {
                result.get(10, TimeUnit.SECONDS);
            }
        }

        // Lượt nào thắng cuối cùng cũng được, miễn tổng sao của truyện đúng bằng số sao đang lưu của người này
        Integer storedStars = jdbcTemplate.queryForObject(
                "SELECT stars FROM story_rating WHERE story_id = ? AND user_id = ?", Integer.class, story.getId(), readerId);
        assertThat(storyCounter(story, "rating_count")).isEqualTo(1);
        assertThat(storyCounter(story, "rating_sum")).isEqualTo(storedStars.longValue());
    }

    @Test
    void followedStory_appearsInLibrary_andDisappearsWhenHidden() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        AppUserPrincipal reader = principalOf(Role.USER);
        follow(story, reader);

        mockMvc.perform(get("/me/library").with(user(reader)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(story.getTitle())));
        mockMvc.perform(get("/stories/{slug}", story.getSlug()).with(user(reader)))
                .andExpect(content().string(containsString("data-following=\"true\"")));

        story.setVisibility(StoryVisibility.HIDDEN);
        storyRepository.saveAndFlush(story);

        mockMvc.perform(get("/me/library").with(user(reader)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(story.getTitle()))));
        // Truyện đã bị ẩn vẫn bỏ theo dõi được, để người dùng dọn tủ truyện của mình
        unfollow(story, reader).andExpect(status().isOk());
    }

    @Test
    void follow_ofNonPublicOrMissingStory_is404() throws Exception {
        Story draft = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.DRAFT));
        AppUserPrincipal reader = principalOf(Role.USER);

        follow(draft, reader).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STORY_NOT_FOUND"));
        mockMvc.perform(delete("/api/stories/{id}/follow", Long.MAX_VALUE).with(user(reader)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(storyCounter(draft, "follow_count")).isZero();
    }

    @Test
    void rating_addsOnlyTheDifference_whenUserChangesTheirScore() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        AppUserPrincipal reader = principalOf(Role.USER);
        AppUserPrincipal otherReader = principalOf(Role.USER);

        rate(story, reader, 4).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myStars").value(4))
                .andExpect(jsonPath("$.data.ratingAverage").value(4.0))
                .andExpect(jsonPath("$.data.ratingCount").value(1));
        // Sửa điểm: số lượt giữ nguyên, tổng sao chỉ đổi đúng phần chênh lệch
        rate(story, reader, 2).andExpect(jsonPath("$.data.ratingAverage").value(2.0))
                .andExpect(jsonPath("$.data.ratingCount").value(1));
        rate(story, otherReader, 5).andExpect(jsonPath("$.data.ratingAverage").value(3.5))
                .andExpect(jsonPath("$.data.ratingCount").value(2));

        assertThat(storyCounter(story, "rating_sum")).isEqualTo(7);
        assertThat(storyCounter(story, "rating_count")).isEqualTo(2);
        mockMvc.perform(get("/stories/{slug}", story.getSlug()).with(user(reader)))
                .andExpect(content().string(containsString("data-my-stars=\"2\"")));
    }

    @Test
    void rating_rejectsOutOfRangeStars_ownStory_andNonPublicStory() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Story draft = createStory(StoryType.NOVEL, created -> created.setVisibility(StoryVisibility.DRAFT));
        AppUserPrincipal reader = principalOf(Role.USER);

        rate(story, reader, 0).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("stars"));
        rate(story, reader, 6).andExpect(status().isBadRequest());
        rate(story, principalOf(story.getAuthor()), 5).andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RATING_OWN_STORY"));
        rate(draft, reader, 5).andExpect(status().isNotFound());

        assertThat(storyCounter(story, "rating_count")).isZero();
        assertThat(storyCounter(story, "rating_sum")).isZero();
    }

    private ResultActions follow(Story story, AppUserPrincipal principal) throws Exception {
        return mockMvc.perform(put("/api/stories/{id}/follow", story.getId()).with(user(principal)).with(csrf()));
    }

    private ResultActions unfollow(Story story, AppUserPrincipal principal) throws Exception {
        return mockMvc.perform(delete("/api/stories/{id}/follow", story.getId()).with(user(principal)).with(csrf()));
    }

    private ResultActions rate(Story story, AppUserPrincipal principal, int stars) throws Exception {
        return mockMvc.perform(put("/api/stories/{id}/rating", story.getId())
                .with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\": " + stars + "}"));
    }
}
