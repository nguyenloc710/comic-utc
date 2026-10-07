package vn.edu.utc.comic.report.dto;

import java.time.Instant;
import vn.edu.utc.comic.report.enums.ReportReason;
import vn.edu.utc.comic.report.enums.ReportStatus;
import vn.edu.utc.comic.report.enums.ReportTargetType;

/**
 * Một báo cáo vi phạm trong hàng đợi của quản trị viên.
 *
 * @param targetLabel   tên ngắn của nội dung bị báo cáo (tên truyện, "Chương 3 – Tên truyện", đoạn đầu bình luận);
 *                      {@code null} nếu nội dung không còn tồn tại
 * @param targetLink    đường dẫn mở nội dung bị báo cáo; {@code null} nếu không còn
 * @param targetHidden  nội dung hiện đã bị ẩn (hoặc không còn) hay chưa
 */
public record ReportResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        String targetLabel,
        String targetLink,
        boolean targetHidden,
        ReportReason reason,
        String detail,
        ReportStatus status,
        String reporterUsername,
        String handlerUsername,
        Instant handledAt,
        String resolutionNote,
        Instant createdAt) {
}
