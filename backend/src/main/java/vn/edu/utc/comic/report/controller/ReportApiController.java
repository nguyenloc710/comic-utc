package vn.edu.utc.comic.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.report.dto.ReportCreateRequest;
import vn.edu.utc.comic.report.service.ReportService;

/**
 * API gửi báo cáo vi phạm, gọi bằng fetch() từ hộp thoại "Báo cáo" ở trang truyện, trang đọc và bình luận.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_REPORTS_PATH)
@Tag(name = "Báo cáo vi phạm", description = "Độc giả báo cáo truyện, chương hoặc bình luận vi phạm")
public class ReportApiController {

    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gửi báo cáo vi phạm",
            description = "Lỗi: 400 VALIDATION_ERROR, 404 REPORT_TARGET_NOT_FOUND nếu nội dung không tồn tại hoặc "
                    + "không công khai, 409 REPORT_ALREADY_PENDING nếu bạn đã báo cáo nội dung này và chưa được xử lý.")
    public ApiResponse<Void> submit(@Valid @RequestBody ReportCreateRequest request,
                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        reportService.submit(principal.getId(), request);
        return ApiResponse.success(null);
    }
}
