package vn.edu.utc.comic.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.notification.dto.UnreadCountResponse;
import vn.edu.utc.comic.notification.service.NotificationService;

/**
 * API cho biểu tượng chuông: trang nào cũng hỏi lại số thông báo chưa đọc theo chu kỳ.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_NOTIFICATIONS_PATH)
@Tag(name = "Thông báo", description = "Số thông báo chưa đọc của người đang đăng nhập")
public class NotificationApiController {

    private final NotificationService notificationService;

    @GetMapping("/unread-count")
    @Operation(summary = "Số thông báo chưa đọc", description = "Lỗi: 401 UNAUTHORIZED nếu chưa đăng nhập.")
    public ApiResponse<UnreadCountResponse> countUnread(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(new UnreadCountResponse(notificationService.countUnread(principal.getId())));
    }
}
