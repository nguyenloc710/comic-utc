package vn.edu.utc.comic.story.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.chapter.service.ChapterPublishService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.dto.ReasonForm;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.story.dto.AdminStoryFilterRequest;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.story.service.StoryModerationService;

/**
 * Kiểm duyệt truyện và chương: danh sách có lọc, trang chi tiết với nút ẩn / gỡ ẩn kèm lý do.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.ADMIN_STORIES_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminStoryController {

    private static final String ATTR_STORY = "story";
    private static final String ATTR_CHAPTERS = "chapters";
    private static final String ATTR_TYPES = "types";
    private static final String ATTR_VISIBILITIES = "visibilities";
    private static final String PATH_SEPARATOR = "/";

    private final StoryModerationService storyModerationService;
    private final ChapterPublishService chapterPublishService;
    private final FlashMessages flash;

    /** Mọi truyện (kể cả nháp, bị ẩn, đã xóa); bộ lọc nằm trên query string. */
    @GetMapping
    public String listStories(@ModelAttribute(ViewConstants.ATTR_FILTER) AdminStoryFilterRequest filter,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, storyModerationService.searchStories(filter, page));
        model.addAttribute(ATTR_TYPES, StoryType.values());
        model.addAttribute(ATTR_VISIBILITIES, StoryVisibility.values());
        return ViewConstants.ADMIN_STORIES;
    }

    /** Chi tiết truyện kèm mọi chương; mỗi thứ có form ẩn (lý do) hoặc nút gỡ ẩn. */
    @GetMapping("/{id}")
    public String showStory(@PathVariable Long id, Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, new ReasonForm());
        return showDetail(id, model);
    }

    /** Ẩn truyện; thiếu lý do thì hiện lại trang chi tiết kèm lỗi ở ô nhập. */
    @PostMapping("/{id}/hide")
    public String hideStory(@PathVariable Long id, @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ReasonForm form,
                            BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return showDetail(id, model);
        }
        try {
            flash.success(redirect, MessageKeys.FLASH_STORY_HIDDEN, storyModerationService.hideStory(id, form.getReason()));
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return redirectToStory(id);
    }

    /** Gỡ ẩn truyện. */
    @PostMapping("/{id}/unhide")
    public String unhideStory(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            flash.success(redirect, MessageKeys.FLASH_STORY_UNHIDDEN, storyModerationService.unhideStory(id));
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return redirectToStory(id);
    }

    /** Ẩn một chương đã đăng của truyện. Lý do thiếu thì báo ở flash (form chương nằm trong bảng, không có ô lỗi riêng). */
    @PostMapping("/{id}/chapters/{chapterId}/hide")
    public String hideChapter(@PathVariable Long id, @PathVariable Long chapterId,
                              @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ReasonForm form,
                              BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            flash.error(redirect, new ApiException(ErrorCode.REASON_REQUIRED));
            return redirectToStory(id);
        }
        try {
            chapterPublishService.hideChapter(chapterId, form.getReason());
            flash.success(redirect, MessageKeys.FLASH_CHAPTER_HIDDEN);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return redirectToStory(id);
    }

    /** Gỡ ẩn một chương. */
    @PostMapping("/{id}/chapters/{chapterId}/unhide")
    public String unhideChapter(@PathVariable Long id, @PathVariable Long chapterId, RedirectAttributes redirect) {
        try {
            chapterPublishService.unhideChapter(chapterId);
            flash.success(redirect, MessageKeys.FLASH_CHAPTER_UNHIDDEN);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return redirectToStory(id);
    }

    private String showDetail(Long storyId, Model model) {
        model.addAttribute(ATTR_STORY, storyModerationService.getStory(storyId));
        model.addAttribute(ATTR_CHAPTERS, storyModerationService.findChapters(storyId));
        return ViewConstants.ADMIN_STORY_DETAIL;
    }

    private static String redirectToStory(Long storyId) {
        return ViewConstants.redirectTo(ApiConstants.ADMIN_STORIES_PATH + PATH_SEPARATOR + storyId);
    }
}
