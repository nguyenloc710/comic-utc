package vn.edu.utc.comic.common.constant;

/**
 * Hằng số cố định của chatbot. Tham số đổi được khi vận hành (bật/tắt, hạn mức, cửa sổ lịch sử...) nằm ở bảng
 * setting; giá trị ở đây chỉ dùng khi bảng setting thiếu khóa.
 */
public final class ChatbotConstants {

    /** Trần cứng độ dài tin nhắn ở tầng nhận request; giới hạn thật (nhỏ hơn) đọc từ setting. */
    public static final int MESSAGE_HARD_MAX_LENGTH = 2000;

    /** Số ký tự đầu của tin nhắn đầu tiên dùng làm tên hội thoại. */
    public static final int CONVERSATION_TITLE_LENGTH = 80;

    /** Mô hình xin bao nhiêu truyện cũng chỉ trả tối đa chừng này, để kết quả hàm gọn (tiết kiệm token). */
    public static final int TOOL_MAX_LIMIT = 10;
    public static final int TOOL_DEFAULT_LIMIT = 6;

    /** Mô tả truyện trong kết quả hàm được cắt ngắn: đủ để mô hình hiểu nội dung, không phí token. */
    public static final int TOOL_SHORT_DESCRIPTION_LENGTH = 200;
    public static final int TOOL_DETAIL_DESCRIPTION_LENGTH = 1000;

    /** Số hội thoại gần nhất trả về cho khung chat. */
    public static final int CONVERSATION_LIST_SIZE = 20;

    /** Số tin nhắn tối đa nạp lại khi mở một hội thoại cũ. */
    public static final int CONVERSATION_MESSAGE_LIMIT = 100;

    /** Số câu trả lời bị chấm "không hữu ích" gần nhất hiện ở trang quản trị chatbot. */
    public static final int ADMIN_RECENT_NEGATIVE_SIZE = 10;

    /** Khoảng thời gian thống kê trên trang quản trị chatbot. */
    public static final int ADMIN_STATS_DAYS = 30;

    /** Mỗi lượt dọn hội thoại cũ xóa tối đa chừng này hội thoại một lần, để câu DELETE không khóa bảng quá lâu. */
    public static final int CLEANUP_BATCH_SIZE = 500;
    public static final String CLEANUP_JOB_NAME = "chat-cleanup";

    /** Dùng khi bảng setting chưa có khóa tương ứng. */
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_DAILY_LIMIT = 50;
    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 500;
    public static final int DEFAULT_HISTORY_WINDOW = 10;
    public static final int DEFAULT_MAX_RECOMMENDATIONS = 6;
    public static final int DEFAULT_RETENTION_DAYS = 90;

    private ChatbotConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
