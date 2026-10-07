package vn.edu.utc.comic.report.dto;

/**
 * Nội dung bị báo cáo, đã tra từ bảng tương ứng.
 *
 * @param label  tên ngắn để hiển thị
 * @param link   đường dẫn mở nội dung
 * @param hidden nội dung đang bị ẩn (không còn ai thấy)
 */
public record ReportTarget(String label, String link, boolean hidden) {

    /** Nội dung không còn tồn tại (đã xóa). */
    public static ReportTarget missing() {
        return new ReportTarget(null, null, true);
    }
}
