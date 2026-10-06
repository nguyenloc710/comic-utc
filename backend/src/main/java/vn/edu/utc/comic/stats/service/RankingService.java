package vn.edu.utc.comic.stats.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.CacheConstants;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.constant.StoryConstants;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.stats.enums.RankingType;
import vn.edu.utc.comic.stats.repository.StoryViewDailyRepository;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Bảng xếp hạng truyện.
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    private final StoryViewDailyRepository storyViewDailyRepository;
    private final StoryCatalogQueryService storyCatalogQueryService;
    private final SettingService settingService;
    private final Clock clock;

    /**
     * Các truyện đứng đầu theo một tiêu chí, hạng cao nhất trước.
     *
     * <p>Kết quả được cache 10 phút (xem CacheConfig): truy vấn gom nhóm lượt xem khá nặng, mà bảng xếp hạng
     * không cần đúng từng phút. Hệ quả cần biết: truyện vừa bị ẩn có thể còn nằm trong bảng tối đa 10 phút —
     * bấm vào sẽ ra trang 404.
     */
    @Cacheable(value = CacheConstants.RANKINGS, key = "#type")
    @Transactional(readOnly = true)
    public List<StoryCardResponse> getRanking(RankingType type) {
        if (type.isByViews()) {
            return rankByRecentViews(type.getViewWindowDays());
        }
        if (type == RankingType.FOLLOWS) {
            return storyCatalogQueryService.findTopStories(StoryFilterRequest.sortedBy(StorySort.FOLLOWS),
                    ApiConstants.RANKING_SIZE);
        }
        // Truyện mới có một lượt 5 sao không được đứng trên truyện có hàng trăm lượt 4,8 sao
        int minRatingCount = settingService.getInt(SettingKeys.RANKING_MIN_RATING_COUNT,
                StoryConstants.DEFAULT_RANKING_MIN_RATING_COUNT);
        return storyCatalogQueryService.findTopRated(StoryFilterRequest.sortedBy(StorySort.RATING),
                minRatingCount, ApiConstants.RANKING_SIZE);
    }

    private List<StoryCardResponse> rankByRecentViews(int windowDays) {
        // Lượt xem được gom theo ngày của giờ Việt Nam, nên mốc bắt đầu cũng phải tính theo giờ Việt Nam
        LocalDate today = LocalDate.now(clock.withZone(DateTimeConstants.DISPLAY_ZONE));
        List<Long> storyIds = storyViewDailyRepository.findTopViewedStoryIds(
                today.minusDays(windowDays - 1L), PageRequest.of(0, ApiConstants.RANKING_SIZE));
        return storyCatalogQueryService.findCardsByIds(storyIds);
    }
}
