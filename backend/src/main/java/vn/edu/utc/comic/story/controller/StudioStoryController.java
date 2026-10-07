package vn.edu.utc.comic.story.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.common.web.FormErrors;
import vn.edu.utc.comic.story.dto.StoryForm;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.service.StudioStoryService;

/**
 * Quản lý truyện của tác giả đang đăng nhập: danh sách, tạo, sửa, công khai, xóa.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.STUDIO_STORIES_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_AUTHOR)
public class StudioStoryController {

    private static final String ATTR_STORY = "story";
    private static final String ATTR_GENRES = "genres";
    private static final String ATTR_TYPES = "types";
    private static final String ATTR_STATUSES = "statuses";
    private static final String ATTR_TYPE_LOCKED = "typeLocked";
    private static final String FIELD_COVER = "cover";
    private static final String CHAPTERS_SUFFIX = "/chapters";

    private final StudioStoryService studioStoryService;
    private final FlashMessages flash;

    /** Danh sách truyện của tác giả kèm trạng thái và số liệu. */
    @GetMapping
    public String listStories(@AuthenticationPrincipal AppUserPrincipal principal,
                              @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, studioStoryService.findStories(principal.getId(), page));
        return ViewConstants.STUDIO_STORIES;
    }

    /** Form tạo truyện. */
    @GetMapping("/new")
    public String showCreateForm(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, new StoryForm());
        return showForm(null, principal, model);
    }

    /** Tạo truyện (bản nháp) rồi chuyển sang trang chương của truyện để tác giả thêm chương đầu tiên. */
    @PostMapping
    public String createStory(@AuthenticationPrincipal AppUserPrincipal principal,
                              @Valid @ModelAttribute(ViewConstants.ATTR_FORM) StoryForm form,
                              BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        Long storyId = null;
        if (!bindingResult.hasErrors()) {
            storyId = saveStory(null, principal, form, bindingResult);
        }
        if (bindingResult.hasErrors()) {
            return showForm(null, principal, model);
        }
        flash.success(redirect, MessageKeys.FLASH_STORY_CREATED);
        return ViewConstants.redirectTo(ApiConstants.STUDIO_STORIES_PATH + "/" + storyId + CHAPTERS_SUFFIX);
    }

    /** Form sửa truyện điền sẵn giá trị hiện tại. */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                               Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, studioStoryService.getForm(id, principal.getId()));
        return showForm(id, principal, model);
    }

    /** Lưu thay đổi của truyện; lỗi thì hiện lại form, thành công thì về danh sách (PRG). */
    @PostMapping("/{id}")
    public String updateStory(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                              @Valid @ModelAttribute(ViewConstants.ATTR_FORM) StoryForm form,
                              BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (!bindingResult.hasErrors()) {
            saveStory(id, principal, form, bindingResult);
        }
        if (bindingResult.hasErrors()) {
            return showForm(id, principal, model);
        }
        flash.success(redirect, MessageKeys.FLASH_STORY_UPDATED);
        return ViewConstants.REDIRECT_STUDIO_STORIES;
    }

    /** Công khai truyện đang là bản nháp; còn thiếu thông tin thì báo ở thông báo flash. */
    @PostMapping("/{id}/publish")
    public String publishStory(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                               RedirectAttributes redirect) {
        try {
            flash.success(redirect, MessageKeys.FLASH_STORY_PUBLISHED,
                    studioStoryService.publishStory(id, principal.getId()));
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_STUDIO_STORIES;
    }

    /** Xóa (mềm) truyện rồi quay lại danh sách. */
    @PostMapping("/{id}/delete")
    public String deleteStory(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                              RedirectAttributes redirect) {
        flash.success(redirect, MessageKeys.FLASH_STORY_DELETED,
                studioStoryService.deleteStory(id, principal.getId()));
        return ViewConstants.REDIRECT_STUDIO_STORIES;
    }

    /**
     * Gọi service tạo / sửa và đưa lỗi nghiệp vụ về đúng ô nhập: lỗi theo trường thì theo tên trường,
     * lỗi ảnh (chỉ có một thông báo chung) thì gắn vào ô chọn ảnh bìa.
     *
     * @return id truyện; {@code null} nếu có lỗi
     */
    private Long saveStory(Long storyId, AppUserPrincipal principal, StoryForm form, BindingResult bindingResult) {
        try {
            if (storyId == null) {
                return studioStoryService.createStory(principal.getId(), form);
            }
            studioStoryService.updateStory(storyId, principal.getId(), form);
            return storyId;
        } catch (FieldValidationException exception) {
            FormErrors.apply(bindingResult, exception);
        } catch (ApiException exception) {
            if (!exception.getErrorCode().isImageError()) {
                throw exception;
            }
            FormErrors.apply(bindingResult, FIELD_COVER, exception);
        }
        return null;
    }

    private String showForm(Long storyId, AppUserPrincipal principal, Model model) {
        if (storyId != null) {
            model.addAttribute(ATTR_STORY, studioStoryService.getStory(storyId, principal.getId()));
        }
        model.addAttribute(ATTR_GENRES, studioStoryService.findGenreOptions(storyId, principal.getId()));
        model.addAttribute(ATTR_TYPE_LOCKED, storyId != null && studioStoryService.isTypeLocked(storyId, principal.getId()));
        model.addAttribute(ATTR_TYPES, StoryType.values());
        model.addAttribute(ATTR_STATUSES, StoryStatus.values());
        return ViewConstants.STUDIO_STORY_FORM;
    }
}
