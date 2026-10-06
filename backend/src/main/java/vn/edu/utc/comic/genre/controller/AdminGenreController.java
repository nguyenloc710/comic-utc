package vn.edu.utc.comic.genre.controller;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.web.FlashMessages;
import vn.edu.utc.comic.common.web.FormErrors;
import vn.edu.utc.comic.genre.dto.GenreForm;
import vn.edu.utc.comic.genre.service.GenreService;

/**
 * Quản trị thể loại: danh sách, tạo, sửa, xóa (chỉ khi chưa có truyện nào dùng).
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(ApiConstants.ADMIN_GENRES_PATH)
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminGenreController {

    private static final String ATTR_GENRES = "genres";
    private static final String ATTR_GENRE_ID = "genreId";

    private final GenreService genreService;
    private final FlashMessages flash;

    /** Danh sách toàn bộ thể loại kèm số truyện đang gắn. */
    @GetMapping
    public String listGenres(Model model) {
        model.addAttribute(ATTR_GENRES, genreService.findAllForAdmin());
        return ViewConstants.ADMIN_GENRES;
    }

    /** Form tạo thể loại. */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, new GenreForm());
        return ViewConstants.ADMIN_GENRE_FORM;
    }

    /** Tạo thể loại; lỗi thì hiện lại form, thành công thì về danh sách (PRG). */
    @PostMapping
    public String createGenre(@Valid @ModelAttribute(ViewConstants.ATTR_FORM) GenreForm form,
                              BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return ViewConstants.ADMIN_GENRE_FORM;
        }
        try {
            flash.success(redirect, MessageKeys.FLASH_GENRE_CREATED, genreService.createGenre(form));
        } catch (FieldValidationException exception) {
            FormErrors.apply(bindingResult, exception);
            return ViewConstants.ADMIN_GENRE_FORM;
        }
        return ViewConstants.REDIRECT_ADMIN_GENRES;
    }

    /** Form sửa thể loại điền sẵn giá trị hiện tại. */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute(ViewConstants.ATTR_FORM, genreService.getForm(id));
        model.addAttribute(ATTR_GENRE_ID, id);
        return ViewConstants.ADMIN_GENRE_FORM;
    }

    /** Lưu thay đổi của thể loại; lỗi thì hiện lại form, thành công thì về danh sách (PRG). */
    @PostMapping("/{id}")
    public String updateGenre(@PathVariable Long id, @Valid @ModelAttribute(ViewConstants.ATTR_FORM) GenreForm form,
                              BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (!bindingResult.hasErrors()) {
            try {
                genreService.updateGenre(id, form);
            } catch (FieldValidationException exception) {
                FormErrors.apply(bindingResult, exception);
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_GENRE_ID, id);
            return ViewConstants.ADMIN_GENRE_FORM;
        }
        flash.success(redirect, MessageKeys.FLASH_GENRE_UPDATED);
        return ViewConstants.REDIRECT_ADMIN_GENRES;
    }

    /** Xóa thể loại chưa có truyện nào dùng; còn truyện thì báo lỗi ở thông báo flash. */
    @PostMapping("/{id}/delete")
    public String deleteGenre(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            genreService.deleteGenre(id);
            flash.success(redirect, MessageKeys.FLASH_GENRE_DELETED);
        } catch (ApiException exception) {
            flash.error(redirect, exception);
        }
        return ViewConstants.REDIRECT_ADMIN_GENRES;
    }
}
