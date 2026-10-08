package vn.edu.utc.comic.story.controller;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.utc.comic.chapter.dto.ChapterSummaryResponse;
import vn.edu.utc.comic.chapter.service.ChapterReaderService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.interaction.service.ReadingHistoryService;
import vn.edu.utc.comic.stats.enums.RankingType;
import vn.edu.utc.comic.stats.service.RankingService;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.dto.StoryUpdateResponse;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Trang chủ theo bố cục quen thuộc của các trang đọc truyện: hàng truyện đề cử, lưới "Truyện mới cập nhật" (mỗi
 * truyện kèm vài chương mới nhất), cột phải gồm bảng xếp hạng ngày / tuần / tháng và lịch sử đọc.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final String ATTR_FEATURED = "featuredStories";
    private static final String ATTR_UPDATES = "updates";
    private static final String ATTR_TOP_DAY = "topDay";
    private static final String ATTR_TOP_WEEK = "topWeek";
    private static final String ATTR_TOP_MONTH = "topMonth";
    private static final String ATTR_CONTINUE_READING = "continueReading";

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final ChapterReaderService chapterReaderService;
    private final RankingService rankingService;
    private final ReadingHistoryService readingHistoryService;

    /** Hiển thị trang chủ; người đã đăng nhập có thêm khối lịch sử đọc ở cột phải. */
    @GetMapping(ApiConstants.HOME_PATH)
    public String showHome(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ATTR_FEATURED, top(RankingType.WEEK, ApiConstants.HOME_FEATURED_SIZE));
        model.addAttribute(ATTR_UPDATES, findUpdates());
        model.addAttribute(ATTR_TOP_MONTH, top(RankingType.MONTH, ApiConstants.HOME_TOP_SIZE));
        model.addAttribute(ATTR_TOP_WEEK, top(RankingType.WEEK, ApiConstants.HOME_TOP_SIZE));
        model.addAttribute(ATTR_TOP_DAY, top(RankingType.DAY, ApiConstants.HOME_TOP_SIZE));
        model.addAttribute(ATTR_CONTINUE_READING, principal == null
                ? List.of()
                : readingHistoryService.findRecent(principal.getId(), ApiConstants.CONTINUE_READING_SIZE));
        return ViewConstants.HOME;
    }

    /** Truyện mới cập nhật, mỗi truyện kèm các chương mới nhất (lấy bằng một truy vấn cho cả trang). */
    private List<StoryUpdateResponse> findUpdates() {
        List<StoryCardResponse> stories = storyCatalogQueryService.findTopStories(
                StoryFilterRequest.sortedBy(StorySort.UPDATED), ApiConstants.HOME_UPDATES_SIZE);
        Map<Long, List<ChapterSummaryResponse>> chapters = chapterReaderService.findLatestChapters(
                stories.stream().map(StoryCardResponse::id).toList(), ApiConstants.HOME_LATEST_CHAPTERS);
        return stories.stream()
                .map(story -> new StoryUpdateResponse(story, chapters.getOrDefault(story.id(), List.of())))
                .toList();
    }

    private List<StoryCardResponse> top(RankingType type, int size) {
        return rankingService.getRanking(type).stream().limit(size).toList();
    }
}
