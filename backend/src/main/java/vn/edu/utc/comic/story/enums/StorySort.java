package vn.edu.utc.comic.story.enums;

/**
 * Tiêu chí sắp xếp danh sách truyện mà người dùng chọn được.
 * Khi có từ khóa mà không chọn tiêu chí nào thì kết quả xếp theo độ khớp với từ khóa.
 */
public enum StorySort {
    /** Mới ra chương gần đây nhất. */
    UPDATED,
    /** Mới được đăng lên. */
    NEWEST,
    VIEWS,
    FOLLOWS,
    RATING
}
