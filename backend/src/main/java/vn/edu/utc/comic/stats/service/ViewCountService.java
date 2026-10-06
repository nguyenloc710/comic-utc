package vn.edu.utc.comic.stats.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.edu.utc.comic.chapter.event.ChapterViewedEvent;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.config.AsyncSchedulingConfig;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.constant.StoryConstants;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.stats.repository.StoryViewDailyRepository;
import vn.edu.utc.comic.story.repository.StoryRepository;

/**
 * Đếm lượt xem chương.
 *
 * <p>Chạy nền, nghe {@link ChapterViewedEvent}: trang đọc không phải chờ ba câu UPDATE. Mất vài lượt xem khi
 * máy chủ tắt đột ngột là chấp nhận được với một con số thống kê.
 *
 * <p>Ba câu UPDATE cố ý KHÔNG nằm chung một transaction (mỗi câu tự commit ở repository). Nếu gộp lại, luồng
 * này sẽ giữ khóa dòng chương trong lúc chờ khóa dòng truyện, còn request vừa lưu tiến độ đọc hay bình luận
 * lại giữ khóa dòng truyện trong lúc chờ dòng chương (kiểm tra khóa ngoại) — hai bên chờ nhau và MySQL hủy
 * một bên. Mỗi câu một transaction thì luồng này không bao giờ vừa giữ khóa vừa chờ khóa khác. Cái giá là ba
 * bộ đếm có thể lệch nhau một lượt nếu máy chủ tắt giữa chừng, chấp nhận được với số thống kê.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    /** Chỉ là trần bộ nhớ; khoảng khử trùng lặp thật sự đọc từ setting ở mỗi lượt xem. */
    private static final Duration RECENT_VIEW_RETENTION = Duration.ofHours(24);
    private static final long RECENT_VIEW_MAX_ENTRIES = 200_000;

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryViewDailyRepository storyViewDailyRepository;
    private final SettingService settingService;
    private final Clock clock;

    /** Lần gần nhất mỗi người xem được tính một lượt cho mỗi chương: "người xem|chương" → thời điểm. */
    private final Cache<String, Instant> recentViews = Caffeine.newBuilder()
            .expireAfterWrite(RECENT_VIEW_RETENTION)
            .maximumSize(RECENT_VIEW_MAX_ENTRIES)
            .build();

    /**
     * Tính một lượt xem, trừ khi cùng người xem đã được tính cho chính chương này trong khoảng khử trùng lặp
     * (đọc từ setting) — tải lại trang hay lật qua lật lại giữa hai chương không làm tăng lượt xem.
     */
    @Async(AsyncSchedulingConfig.EVENT_EXECUTOR)
    @EventListener
    public void handleChapterViewed(ChapterViewedEvent event) {
        if (!markAsCounted(event)) {
            return;
        }
        chapterRepository.incrementViewCount(event.chapterId());
        storyRepository.incrementViewCount(event.storyId());
        // Ngày tính theo giờ Việt Nam để "lượt xem hôm nay" khớp với ngày của người đọc
        storyViewDailyRepository.addDailyView(event.storyId(),
                LocalDate.now(clock.withZone(DateTimeConstants.DISPLAY_ZONE)));
        log.debug("Đã tính một lượt xem cho chương {}", event.chapterId());
    }

    /**
     * @return {@code true} nếu lượt xem này được tính (và thời điểm vừa được ghi lại)
     */
    private boolean markAsCounted(ChapterViewedEvent event) {
        Duration dedupeWindow = Duration.ofMinutes(settingService.getInt(SettingKeys.VIEW_DEDUPE_MINUTES,
                StoryConstants.DEFAULT_VIEW_DEDUPE_MINUTES));
        Instant now = clock.instant();
        AtomicBoolean counted = new AtomicBoolean(false);
        // compute() là nguyên tử theo khóa: hai request đồng thời của cùng một người không cùng được tính
        recentViews.asMap().compute(event.visitorKey() + "|" + event.chapterId(), (key, lastCountedAt) -> {
            if (lastCountedAt != null && lastCountedAt.plus(dedupeWindow).isAfter(now)) {
                return lastCountedAt;
            }
            counted.set(true);
            return now;
        });
        return counted.get();
    }
}
