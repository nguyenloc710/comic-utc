package vn.edu.utc.comic.chapter.enums;

/**
 * Nút mà tác giả bấm trên form soạn chương. Cả ba đều LƯU nội dung trước; khác nhau ở việc làm sau đó.
 */
public enum ChapterFormAction {
    /** Chỉ lưu, giữ nguyên trạng thái chương. */
    SAVE,
    /** Lưu rồi đăng ngay. */
    PUBLISH,
    /** Lưu rồi hẹn giờ đăng theo ô ngày giờ trên form. */
    SCHEDULE
}
