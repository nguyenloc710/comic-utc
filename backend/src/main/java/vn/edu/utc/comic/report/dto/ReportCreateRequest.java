package vn.edu.utc.comic.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.utc.comic.common.constant.ReportConstants;
import vn.edu.utc.comic.report.enums.ReportReason;
import vn.edu.utc.comic.report.enums.ReportTargetType;

/** Nội dung một báo cáo vi phạm do độc giả gửi. */
@Schema(description = "Báo cáo vi phạm về một truyện, chương hoặc bình luận")
public record ReportCreateRequest(
        @NotNull(message = "{validation.report.target.required}") ReportTargetType targetType,
        @NotNull(message = "{validation.report.target.required}") Long targetId,
        @NotNull(message = "{validation.report.reason.required}") ReportReason reason,
        @Size(max = ReportConstants.DETAIL_MAX_LENGTH, message = "{validation.report.detail.size}")
        @Schema(description = "Mô tả thêm, có thể bỏ trống") String detail) {
}
