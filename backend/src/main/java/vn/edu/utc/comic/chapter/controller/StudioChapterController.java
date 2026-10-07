package vn.edu.utc.comic.chapter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.chapter.dto.ChapterForm;
import vn.edu.utc.comic.chapter.dto.StudioChapterResponse;
import vn.edu.utc.comic.chapter.enums.ChapterFormAction;
import vn.edu.utc.comic.chapter.service.ChapterPublishService;
import vn.edu.utc.comic.chapter.service.StudioChapterService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.common.web.FormErrors;
import vn.edu.utc.comic.story.dto.StudioStoryResponse;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.service.StudioStoryService;

/**
 * Chương trong khu vực tác giả: danh sách chương của một truyện, trình soạn chương, đăng / hẹn giờ / hủy hẹn.
 *
 * <p>Form soạn chương có ba nút (lưu, đăng ngay, hẹn giờ). Cả ba đều lưu nội dung TRƯỚC rồi mới đổi trạng thái,
 * mỗi bước một transaction: nếu bước đăng bị từ chối (ví dụ chương chưa có ảnh) thì phần vừa soạn vẫn được giữ.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_AUTHOR)
public class StudioChapterController {

    private static final String STORY_CHAPTERS_PATH = ApiConstants.STUDIO_STORIES_PATH + "/{storyId}/chapters";
    private static final String CHAPTER_PATH = ApiConstants.STUDIO_CHAPTERS_PATH + "/{chapterId}";
    private static final String ATTR_STORY = "story";
    private static final String ATTR_CHAPTER = "chapter";
    private static final String ATTR_CHAPTERS = "chapters";
    private static final String PATH_SEPARATOR = "/";
    private static final String CHAPTERS_SUFFIX = "/chapters";
    private static final String EDIT_SUFFIX = "/edit";

    private final StudioChapterService studioChapterService;
    private final StudioStoryService studioStoryService;
    private final ChapterPublishService chapterPublishService;
    private final FlashMessages flash;

    /** Mọi chương của một truyện kèm trạng thái. */
    @GetMapping(STORY_CHAPTERS_PATH)
    public String listChapters(@PathVariable Long storyId, @AuthenticationPrincipal AppUserPrincipal principal,
                               Model model) {
        model.addAttribute(ATTR_STORY, studioStoryService.getStory(storyId, principal.getId()));
        model.addAttribute(ATTR_CHAPTERS, studioChapterService.findChapters(storyId, principal.getId()));
        return ViewConstants.STUDIO_CHAPTERS;
    }

    /** Trình soạn cho chương mới, gợi ý sẵn số chương kế tiếp. */
    @GetMapping(STORY_CHAPTERS_PATH + "/new")
    public String showCreateForm(@PathVariable Long storyId, @AuthenticationPrincipal AppUserPrincipal principal,
                                 Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, studioChapterService.getNewForm(storyId, principal.getId()));
        return showEditor(studioStoryService.getStory(storyId, principal.getId()), null, model);
    }

    /** Tạo chương (bản nháp), thực hiện tiếp nút đã bấm rồi mở trình soạn của chương vừa tạo. */
    @PostMapping(STORY_CHAPTERS_PATH)
    public String createChapter(@PathVariable Long storyId, @AuthenticationPrincipal AppUserPrincipal principal,
                                @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ChapterForm form,
                                BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        Long chapterId = null;
        if (!bindingResult.hasErrors()) {
            try {
                chapterId = studioChapterService.createChapter(storyId, principal.getId(), form);
            } catch (FieldValidationException exception) {
                FormErrors.apply(bindingResult, exception);
            }
        }
        if (bindingResult.hasErrors()) {
            return showEditor(studioStoryService.getStory(storyId, principal.getId()), null, model);
        }
        applyAction(chapterId, principal.getId(), form, redirect);
        return redirectToEditor(chapterId);
    }

    /** Trình soạn của một chương đã có. */
    @GetMapping(CHAPTER_PATH + EDIT_SUFFIX)
    public String showEditForm(@PathVariable Long chapterId, @AuthenticationPrincipal AppUserPrincipal principal,
                               Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, studioChapterService.getForm(chapterId, principal.getId()));
        return showEditor(chapterId, principal, model);
    }

    /** Lưu chương, thực hiện tiếp nút đã bấm rồi quay lại trình soạn (PRG). */
    @PostMapping(CHAPTER_PATH)
    public String updateChapter(@PathVariable Long chapterId, @AuthenticationPrincipal AppUserPrincipal principal,
                                @Valid @ModelAttribute(ViewConstants.ATTR_FORM) ChapterForm form,
                                BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (!bindingResult.hasErrors()) {
            try {
                studioChapterService.updateChapter(chapterId, principal.getId(), form);
            } catch (FieldValidationException exception) {
                FormErrors.apply(bindingResult, exception);
            }
        }
        if (bindingResult.hasErrors()) {
            return showEditor(chapterId, principal, model);
        }
        applyAction(chapterId, principal.getId(), form, redirect);
        return redirectToEditor(chapterId);
    }

    /** Hủy hẹn giờ, đưa chương về bản nháp. */
    @PostMapping(CHAPTER_PATH + "/unschedule")
    public String cancelSchedule(@PathVariable Long chapterId, @AuthenticationPrincipal AppUserPrincipal principal,
                                 RedirectAttributes redirect) {
        try {
            chapterPublishService.cancelSchedule(chapterId, principal.getId());
            flash.success(redirect, MessageKeys.FLASH_CHAPTER_UNSCHEDULED);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return redirectToEditor(chapterId);
    }

    /** Xóa một chương còn là bản nháp rồi quay về danh sách chương của truyện. */
    @PostMapping(CHAPTER_PATH + "/delete")
    public String deleteChapter(@PathVariable Long chapterId, @AuthenticationPrincipal AppUserPrincipal principal,
                                RedirectAttributes redirect) {
        try {
            StudioChapterResponse deleted = studioChapterService.deleteDraftChapter(chapterId, principal.getId());
            flash.success(redirect, MessageKeys.FLASH_CHAPTER_DELETED, deleted.chapterNo());
            return redirectToChapterList(deleted.storyId());
        } catch (ApiException exception) {
            flash.error(redirect, exception);
            return redirectToEditor(chapterId);
        }
    }

    /** Bước thứ hai sau khi đã lưu: đăng ngay hoặc hẹn giờ theo nút tác giả bấm. */
    private void applyAction(Long chapterId, Long authorId, ChapterForm form, RedirectAttributes redirect) {
        try {
            if (form.getAction() == ChapterFormAction.PUBLISH) {
                chapterPublishService.publishNow(chapterId, authorId);
                flash.success(redirect, MessageKeys.FLASH_CHAPTER_PUBLISHED);
            } else if (form.getAction() == ChapterFormAction.SCHEDULE) {
                chapterPublishService.schedule(chapterId, authorId, form.getScheduledAt());
                flash.success(redirect, MessageKeys.FLASH_CHAPTER_SCHEDULED);
            } else {
                flash.success(redirect, MessageKeys.FLASH_CHAPTER_SAVED);
            }
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
    }

    private String showEditor(Long chapterId, AppUserPrincipal principal, Model model) {
        StudioChapterResponse chapter = studioChapterService.getChapter(chapterId, principal.getId());
        return showEditor(studioStoryService.getStory(chapter.storyId(), principal.getId()), chapter, model);
    }

    /** Truyện tranh và truyện chữ có trình soạn khác hẳn nhau (tải ảnh / soạn văn bản) nên dùng hai template. */
    private static String showEditor(StudioStoryResponse story, StudioChapterResponse chapter, Model model) {
        model.addAttribute(ATTR_STORY, story);
        model.addAttribute(ATTR_CHAPTER, chapter);
        return story.type() == StoryType.COMIC
                ? ViewConstants.STUDIO_CHAPTER_COMIC_EDITOR
                : ViewConstants.STUDIO_CHAPTER_NOVEL_EDITOR;
    }

    private static String redirectToEditor(Long chapterId) {
        return ViewConstants.redirectTo(ApiConstants.STUDIO_CHAPTERS_PATH + PATH_SEPARATOR + chapterId + EDIT_SUFFIX);
    }

    private static String redirectToChapterList(Long storyId) {
        return ViewConstants.redirectTo(ApiConstants.STUDIO_STORIES_PATH + PATH_SEPARATOR + storyId + CHAPTERS_SUFFIX);
    }
}
