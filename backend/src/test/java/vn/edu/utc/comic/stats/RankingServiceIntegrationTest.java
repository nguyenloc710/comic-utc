package vn.edu.utc.comic.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import vn.edu.utc.comic.common.constant.CacheConstants;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.stats.enums.RankingType;
import vn.edu.utc.comic.stats.service.RankingService;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;

/**
 * Bảng xếp hạng. Cơ sở dữ liệu dùng chung với test khác, nên test dùng số liệu lớn vượt trội để truyện của
 * mình chắc chắn đứng đầu, và xóa cache trước mỗi lần hỏi (bảng xếp hạng được cache 10 phút).
 */
class RankingServiceIntegrationTest extends AbstractIntegrationTest {

    private static final int HUGE = 50_000_000;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearRankingCache() {
        cacheManager.getCache(CacheConstants.RANKINGS).clear();
    }

    @Test
    void viewRankings_onlyCountViewsInsideTheirWindow() {
        Story hotToday = createPublishedStory(StoryType.NOVEL);
        Story hotLastWeek = createPublishedStory(StoryType.COMIC);
        Story hotLastMonth = createPublishedStory(StoryType.NOVEL);
        addDailyViews(hotToday, 0, HUGE);
        addDailyViews(hotLastWeek, 5, HUGE * 2);
        addDailyViews(hotLastMonth, 20, HUGE * 3);

        assertThat(topIds(RankingType.DAY, 1)).containsExactly(hotToday.getId());
        assertThat(topIds(RankingType.WEEK, 2)).containsExactly(hotLastWeek.getId(), hotToday.getId());
        assertThat(topIds(RankingType.MONTH, 3))
                .containsExactly(hotLastMonth.getId(), hotLastWeek.getId(), hotToday.getId());
    }

    @Test
    void viewRanking_skipsStoriesThatAreNoLongerPublic() {
        Story hidden = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.HIDDEN));
        addDailyViews(hidden, 0, HUGE * 10);

        assertThat(topIds(RankingType.DAY, 20)).doesNotContain(hidden.getId());
    }

    @Test
    void followRanking_ordersByFollowCount() {
        Story followed = createStory(StoryType.NOVEL, story -> story.setFollowCount(HUGE));
        Story moreFollowed = createStory(StoryType.NOVEL, story -> story.setFollowCount(HUGE + 1));

        assertThat(topIds(RankingType.FOLLOWS, 20)).containsSubsequence(moreFollowed.getId(), followed.getId());
    }

    @Test
    void ratingRanking_ignoresStoriesWithTooFewRatings() {
        // Một lượt 5 sao duy nhất không được đứng trên truyện có nhiều lượt đánh giá
        Story fewRatings = createStory(StoryType.NOVEL, story -> {
            story.setRatingSum(5);
            story.setRatingCount(1);
        });
        Story wellRated = createStory(StoryType.NOVEL, story -> {
            story.setRatingSum(HUGE * 5);
            story.setRatingCount(HUGE);
        });

        List<Long> ranking = topIds(RankingType.RATING, 20);

        assertThat(ranking).contains(wellRated.getId()).doesNotContain(fewRatings.getId());
    }

    @Test
    void ranking_isServedFromCache_untilItExpires() {
        List<StoryCardResponse> before = rankingService.getRanking(RankingType.FOLLOWS);
        Story newcomer = createStory(StoryType.NOVEL, story -> story.setFollowCount(HUGE * 2));

        assertThat(rankingService.getRanking(RankingType.FOLLOWS)).isEqualTo(before);

        clearRankingCache();
        assertThat(topIds(RankingType.FOLLOWS, 20)).contains(newcomer.getId());
    }

    private List<Long> topIds(RankingType type, int limit) {
        return rankingService.getRanking(type).stream().limit(limit).map(StoryCardResponse::id).toList();
    }

    private void addDailyViews(Story story, int daysAgo, int views) {
        LocalDate date = LocalDate.now(DateTimeConstants.DISPLAY_ZONE).minusDays(daysAgo);
        jdbcTemplate.update("INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES (?, ?, ?)",
                story.getId(), date, views);
    }
}
