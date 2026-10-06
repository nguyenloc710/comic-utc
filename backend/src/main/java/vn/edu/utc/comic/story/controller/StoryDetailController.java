package vn.edu.utc.comic.story.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.edu.utc.comic.chapter.service.ChapterReaderService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.interaction.service.ViewerStateService;
import vn.edu.utc.comic.story.dto.StoryDetailResponse;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/** Trang chi tiết truyện: thông tin, danh sách chương, nút đọc / theo dõi / đánh giá, bình luận. */
@Controller
@RequiredArgsConstructor
public class StoryDetailController {

    private static final String ATTR_STORY = "story";
    private static final String ATTR_CHAPTERS = "chapters";
    private static final String ATTR_VIEWER_STATE = "viewerState";

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final ChapterReaderService chapterReaderService;
    private final ViewerStateService viewerStateService;

    /**
     * Hiển thị một truyện. Truyện chưa công khai chỉ tác giả của nó và quản trị viên xem trước được;
     * người khác nhận trang 404 như thể truyện không tồn tại.
     */
    @GetMapping(ApiConstants.STORIES_PATH + "/{slug}")
    public String showStory(@PathVariable String slug, @AuthenticationPrincipal AppUserPrincipal principal,
                            Model model) {
        Viewer viewer = Viewer.of(principal);
        StoryDetailResponse story = storyCatalogQueryService.getStoryDetail(slug, viewer);
        model.addAttribute(ATTR_STORY, story);
        model.addAttribute(ATTR_CHAPTERS, chapterReaderService.findPublishedChapters(story.id()));
        model.addAttribute(ATTR_VIEWER_STATE, viewerStateService.getState(story.id(), viewer));
        return ViewConstants.STORY_DETAIL;
    }
}
