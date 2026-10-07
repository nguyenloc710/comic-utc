package vn.edu.utc.comic.common.constant;

/**
 * Hằng số nghiệp vụ của chương. Chỉ chứa giá trị CỐ ĐỊNH; giới hạn đổi được khi vận hành (số ảnh mỗi chương)
 * nằm ở bảng setting.
 */
public final class ChapterConstants {

    /** Khớp độ dài cột chapter.title. */
    public static final int TITLE_MAX_LENGTH = 200;

    public static final int CHAPTER_NO_MIN = 1;
    public static final int CHAPTER_NO_MAX = 99_999;

    /** Trần độ dài HTML của một chương truyện chữ (khoảng 50 nghìn từ), chặn request khổng lồ. */
    public static final int CONTENT_MAX_LENGTH = 400_000;

    /** Dùng khi bảng setting chưa có khóa tương ứng. */
    public static final int DEFAULT_MAX_PAGES = 150;

    /** Tên ghi vào bảng job_run. */
    public static final String PUBLISH_JOB_NAME = "chapter-publish";

    /** Số chương tối đa mỗi lượt chạy job; phần còn lại (nếu có) được đăng ở lượt kế tiếp sau một phút. */
    public static final int PUBLISH_JOB_BATCH_SIZE = 100;

    /** Khớp độ dài cột job_run.error_message. */
    public static final int JOB_ERROR_MESSAGE_MAX_LENGTH = 1000;

    private ChapterConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
