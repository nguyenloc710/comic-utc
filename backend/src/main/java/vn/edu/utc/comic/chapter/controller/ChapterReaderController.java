package vn.edu.utc.comic.chapter.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.edu.utc.comic.chapter.dto.ChapterReadResponse;
import vn.edu.utc.comic.chapter.service.ChapterReaderService;
import vn.edu.utc.comic.chapter.service.ChapterReadingTracker;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.security.Viewer;

/** Trang đọc một chương truyện tranh hoặc truyện chữ. */
@Controller
@RequiredArgsConstructor
public class ChapterReaderController {

    private static final String ATTR_CHAPTER = "chapter";
    private static final String ATTR_CHAPTERS = "chapters";

    private final ChapterReaderService chapterReaderService;
    private final ChapterReadingTracker chapterReadingTracker;

    /**
     * Hiển thị một chương rồi ghi nhận lượt xem và tiến độ đọc. Chương chưa đăng hoặc bị ẩn chỉ tác giả và
     * quản trị viên xem trước được; người khác nhận trang 404.
     *
     * @param session phiên của người xem, để nhận ra cùng một khách vãng lai mở lại chương
     */
    @GetMapping(ApiConstants.STORIES_PATH + "/{slug}/chapters/{chapterNo}")
    public String readChapter(@PathVariable String slug, @PathVariable int chapterNo,
                              @AuthenticationPrincipal AppUserPrincipal principal, HttpSession session,
                              Model model) {
        Viewer viewer = Viewer.of(principal);
        ChapterReadResponse chapter = chapterReaderService.getChapterForReading(slug, chapterNo, viewer);
        chapterReadingTracker.trackRead(chapter, viewer, session.getId());
        model.addAttribute(ATTR_CHAPTER, chapter);
        // Danh sách chương cho ô "chọn chương" trên thanh điều hướng của trang đọc
        model.addAttribute(ATTR_CHAPTERS, chapterReaderService.findPublishedChapters(chapter.storyId()));
        return ViewConstants.CHAPTER_READ;
    }
}
