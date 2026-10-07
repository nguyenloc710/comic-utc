package vn.edu.utc.comic.common.constant;

/**
 * Giới hạn cố định của báo cáo vi phạm và thao tác kiểm duyệt (khớp độ dài cột trong lược đồ).
 */
public final class ReportConstants {

    /** Khớp report.detail và report.resolution_note. */
    public static final int DETAIL_MAX_LENGTH = 1000;
    public static final int RESOLUTION_NOTE_MAX_LENGTH = 1000;

    /** Khớp story.hidden_reason và chapter.hidden_reason. */
    public static final int HIDDEN_REASON_MAX_LENGTH = 1000;
    /** Cột comment.hidden_reason ngắn hơn. */
    public static final int COMMENT_HIDDEN_REASON_MAX_LENGTH = 500;

    /** Số ký tự đầu của bình luận dùng làm nhãn ở hàng đợi báo cáo và nhật ký. */
    public static final int COMMENT_SNIPPET_LENGTH = 80;

    private ReportConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
