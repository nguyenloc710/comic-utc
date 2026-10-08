package vn.edu.utc.comic.chatbot.enums;

/** Vì sao một lượt phải rơi về đường lui tìm theo từ khóa. Thống kê ở trang quản trị chatbot. */
public enum FallbackReason {
    /** Gọi nhà cung cấp quá thời gian chờ. */
    TIMEOUT,
    /** Nhà cung cấp trả lỗi (khóa sai, hết hạn mức, lỗi máy chủ, mất mạng). */
    PROVIDER_ERROR,
    /** Mô hình trả lời nhưng không đọc được thành câu trả lời có cấu trúc. */
    INVALID_RESPONSE
}
