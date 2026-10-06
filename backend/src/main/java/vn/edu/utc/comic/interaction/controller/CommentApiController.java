package vn.edu.utc.comic.interaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.interaction.dto.CommentCreateRequest;
import vn.edu.utc.comic.interaction.dto.CommentResponse;
import vn.edu.utc.comic.interaction.service.CommentService;

/**
 * API bình luận. Xem thì ai cũng xem được (kể cả khách vãng lai); viết và xóa cần đăng nhập.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_COMMENTS_PATH)
@Tag(name = "Bình luận", description = "Xem, viết, trả lời và xóa bình luận ở trang truyện và trang chương")
public class CommentApiController {

    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "Danh sách bình luận",
            description = "Bình luận gốc mới nhất trước, mỗi bình luận kèm các câu trả lời. Không cần đăng nhập. "
                    + "Bỏ trống chapterId để lấy bình luận ở trang truyện. Lỗi: 404 STORY_NOT_FOUND.")
    public ApiResponse<PageResponse<CommentResponse>> listComments(
            @RequestParam Long storyId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(commentService.findComments(storyId, chapterId, page, Viewer.of(principal)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Viết bình luận hoặc trả lời",
            description = "Lỗi: 400 VALIDATION_ERROR / COMMENT_PARENT_INVALID, 404 STORY_NOT_FOUND / CHAPTER_NOT_FOUND, "
                    + "429 COMMENT_TOO_FAST nếu chưa hết thời gian chờ giữa hai bình luận.")
    public ApiResponse<CommentResponse> addComment(@Valid @RequestBody CommentCreateRequest request,
                                                   @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(commentService.addComment(request, principal.getId()));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Xóa bình luận của chính mình",
            description = "Lỗi: 404 COMMENT_NOT_FOUND nếu không có bình luận hoặc bình luận là của người khác.")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId,
                                           @AuthenticationPrincipal AppUserPrincipal principal) {
        commentService.deleteOwnComment(commentId, principal.getId());
        return ApiResponse.success(null);
    }
}
