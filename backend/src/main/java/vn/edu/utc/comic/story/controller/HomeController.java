package vn.edu.utc.comic.story.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.interaction.service.ReadingHistoryService;
import vn.edu.utc.comic.stats.enums.RankingType;
import vn.edu.utc.comic.stats.service.RankingService;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/** Trang chủ công khai: các khối truyện mới cập nhật, nổi bật, mới đăng, hoàn thành. */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final String ATTR_CONTINUE_READING = "continueReading";
    private static final String ATTR_LATEST = "latestStories";
    private static final String ATTR_WEEKLY_TOP = "weeklyTopStories";
    private static final String ATTR_NEWEST = "newestStories";
    private static final String ATTR_COMPLETED = "completedStories";
    private static final int SIDE_SECTION_SIZE = 6;

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final RankingService rankingService;
    private final ReadingHistoryService readingHistoryService;

    /** Hiển thị trang chủ; người đã đăng nhập có thêm khối "Đọc tiếp". */
    @GetMapping(ApiConstants.HOME_PATH)
    public String showHome(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ATTR_CONTINUE_READING, principal == null
                ? List.of()
                : readingHistoryService.findRecent(principal.getId(), ApiConstants.CONTINUE_READING_SIZE));
        model.addAttribute(ATTR_LATEST, storyCatalogQueryService.findTopStories(
                StoryFilterRequest.sortedBy(StorySort.UPDATED), ApiConstants.HOME_SECTION_SIZE));
        model.addAttribute(ATTR_WEEKLY_TOP, rankingService.getRanking(RankingType.WEEK).stream()
                .limit(SIDE_SECTION_SIZE).toList());
        model.addAttribute(ATTR_NEWEST, storyCatalogQueryService.findTopStories(
                StoryFilterRequest.sortedBy(StorySort.NEWEST), SIDE_SECTION_SIZE));
        model.addAttribute(ATTR_COMPLETED, storyCatalogQueryService.findTopStories(
                StoryFilterRequest.ofStatus(StoryStatus.COMPLETED), SIDE_SECTION_SIZE));
        return ViewConstants.HOME;
    }
}
