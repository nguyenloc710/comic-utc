package vn.edu.utc.comic.chapter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.chapter.dto.PageOrderRequest;
import vn.edu.utc.comic.chapter.dto.StudioChapterPageResponse;
import vn.edu.utc.comic.chapter.service.ChapterPageService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.security.AppUserPrincipal;

/**
 * API trang ảnh của chương truyện tranh, gọi bằng fetch() từ trình soạn chương. Tác giả chỉ thao tác được trên
 * chương của truyện mình; chương của người khác trả 404.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_STUDIO_CHAPTERS_PATH + "/{chapterId}/pages")
@PreAuthorize(SecurityConstants.HAS_ROLE_AUTHOR)
@Tag(name = "Trang ảnh của chương", description = "Tải lên, sắp xếp và xóa ảnh của chương truyện tranh")
public class StudioChapterPageApiController {

    private final ChapterPageService chapterPageService;

    @GetMapping
    @Operation(summary = "Danh sách trang của chương theo thứ tự",
            description = "Lỗi: 404 CHAPTER_NOT_FOUND, 409 CHAPTER_TYPE_MISMATCH nếu chương thuộc truyện chữ.")
    public ApiResponse<List<StudioChapterPageResponse>> listPages(
            @PathVariable Long chapterId, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(chapterPageService.findPages(chapterId, principal.getId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tải một ảnh lên cuối chương",
            description = "Mỗi request một ảnh JPG/PNG/WebP (trường 'file'). Lỗi: 400 IMAGE_TYPE_NOT_ALLOWED, "
                    + "400 IMAGE_TOO_LARGE, 404 CHAPTER_NOT_FOUND, 409 CHAPTER_PAGE_LIMIT khi chương đã đủ số ảnh, "
                    + "413 UPLOAD_TOO_LARGE.")
    public ApiResponse<StudioChapterPageResponse> uploadPage(
            @PathVariable Long chapterId, @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(chapterPageService.addPage(chapterId, principal.getId(), file));
    }

    @PutMapping("/order")
    @Operation(summary = "Sắp xếp lại các trang",
            description = "Gửi id của MỌI trang theo thứ tự mới. Lỗi: 400 CHAPTER_PAGE_ORDER_INVALID nếu danh sách "
                    + "không khớp đúng các trang hiện có, 404 CHAPTER_NOT_FOUND.")
    public ApiResponse<List<StudioChapterPageResponse>> reorderPages(
            @PathVariable Long chapterId, @Valid @RequestBody PageOrderRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(chapterPageService.reorderPages(chapterId, principal.getId(), request.pageIds()));
    }

    @DeleteMapping("/{pageId}")
    @Operation(summary = "Xóa một trang",
            description = "Trả về các trang còn lại đã đánh số lại. Lỗi: 404 CHAPTER_PAGE_NOT_FOUND, "
                    + "409 CHAPTER_EMPTY nếu xóa trang cuối cùng của chương đang hẹn giờ hoặc đã đăng.")
    public ApiResponse<List<StudioChapterPageResponse>> deletePage(
            @PathVariable Long chapterId, @PathVariable Long pageId,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(chapterPageService.deletePage(chapterId, pageId, principal.getId()));
    }
}
