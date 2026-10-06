package vn.edu.utc.comic.interaction.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.interaction.service.ReadingHistoryService;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Tủ truyện (truyện đang theo dõi) và lịch sử đọc của người đang đăng nhập.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_USER)
public class LibraryController {

    private static final String ATTR_HISTORY = "history";

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final ReadingHistoryService readingHistoryService;

    /** Truyện đang theo dõi, mới theo dõi trước. */
    @GetMapping(ApiConstants.LIBRARY_PATH)
    public String showLibrary(@AuthenticationPrincipal AppUserPrincipal principal,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE,
                storyCatalogQueryService.findFollowedStories(principal.getId(), page));
        return ViewConstants.ME_LIBRARY;
    }

    /** Truyện đọc gần đây kèm nút đọc tiếp chương đang dở. */
    @GetMapping(ApiConstants.HISTORY_PATH)
    public String showHistory(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ATTR_HISTORY, readingHistoryService.findRecent(principal.getId(), ApiConstants.HISTORY_SIZE));
        return ViewConstants.ME_HISTORY;
    }
}
