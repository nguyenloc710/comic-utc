package vn.edu.utc.comic.stats.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.stats.dto.AuthorStatsOverview;
import vn.edu.utc.comic.stats.dto.DailyViewPoint;
import vn.edu.utc.comic.stats.dto.TopChapterResponse;
import vn.edu.utc.comic.stats.repository.StoryViewDailyRepository;
import vn.edu.utc.comic.story.dto.StudioStoryResponse;
import vn.edu.utc.comic.story.mapper.StoryMapper;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.service.StudioStoryService;

/**
 * Thống kê cho tác giả. Mọi truy vấn đều lọc theo id tác giả lấy từ phiên đăng nhập, nên không ai xem được
 * số liệu truyện của người khác.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorStatsService {

    private static final String SORT_BY_VIEWS = "viewCount";

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryViewDailyRepository storyViewDailyRepository;
    private final StudioStoryService studioStoryService;
    private final StoryMapper storyMapper;
    private final Clock clock;

    /** Tổng lượt xem, theo dõi, bình luận, điểm đánh giá trên mọi truyện của tác giả. */
    public AuthorStatsOverview getOverview(Long authorId) {
        return storyRepository.summarizeByAuthor(authorId);
    }

    /** Số liệu từng truyện của tác giả, truyện được xem nhiều nhất trước. */
    public List<StudioStoryResponse> findStoryStats(Long authorId) {
        return storyMapper.toStudioResponses(storyRepository.findByAuthorIdAndDeletedAtIsNull(authorId,
                Sort.by(Sort.Direction.DESC, SORT_BY_VIEWS)));
    }

    /**
     * Lượt xem theo ngày trong 30 ngày gần nhất (tính cả hôm nay), mỗi ngày đúng một điểm — ngày không có lượt
     * xem nào vẫn có điểm bằng 0 để đường biểu đồ không bị đứt quãng.
     *
     * @param storyId chỉ tính một truyện; {@code null} để cộng mọi truyện của tác giả
     * @throws ApiException STORY_NOT_FOUND nếu truyện không phải của tác giả này
     */
    public List<DailyViewPoint> getDailyViews(Long authorId, Long storyId) {
        if (storyId != null) {
            studioStoryService.getOwnedStory(storyId, authorId);
        }
        // Ngày tính theo giờ Việt Nam, khớp với cách ghi story_view_daily
        LocalDate today = LocalDate.now(clock.withZone(DateTimeConstants.DISPLAY_ZONE));
        LocalDate fromDate = today.minusDays(ApiConstants.STATS_CHART_DAYS - 1L);
        Map<LocalDate, Long> viewsByDate = storyViewDailyRepository.findDailyViewsByAuthor(authorId, storyId, fromDate)
                .stream()
                .collect(Collectors.toMap(DailyViewPoint::date, DailyViewPoint::views));
        return Stream.iterate(fromDate, date -> !date.isAfter(today), date -> date.plusDays(1))
                .map(date -> new DailyViewPoint(date, viewsByDate.getOrDefault(date, 0L)))
                .toList();
    }

    /** Các chương đã đăng được xem nhiều nhất trên mọi truyện của tác giả. */
    public List<TopChapterResponse> findTopChapters(Long authorId) {
        return chapterRepository.findTopChaptersByAuthor(authorId,
                PageRequest.of(0, ApiConstants.STUDIO_TOP_CHAPTER_SIZE));
    }
}
