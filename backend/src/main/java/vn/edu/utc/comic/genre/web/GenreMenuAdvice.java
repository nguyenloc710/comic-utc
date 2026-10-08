package vn.edu.utc.comic.genre.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.utc.comic.genre.dto.GenreTagResponse;
import vn.edu.utc.comic.genre.service.GenreService;

/**
 * Đưa danh sách thể loại vào mọi trang để menu "Thể loại" của layout công khai dựng được. Danh sách lấy từ cache
 * (xóa khi quản trị viên sửa thể loại) nên không thêm truy vấn nào cho mỗi lần mở trang.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GenreMenuAdvice {

    public static final String ATTR_MENU_GENRES = "menuGenres";

    private final GenreService genreService;

    @ModelAttribute(ATTR_MENU_GENRES)
    public List<GenreTagResponse> menuGenres() {
        return genreService.findActiveGenres();
    }
}
