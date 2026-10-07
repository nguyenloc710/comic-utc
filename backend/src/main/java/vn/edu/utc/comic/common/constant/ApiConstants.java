package vn.edu.utc.comic.common.constant;

import java.util.List;

/**
 * Hằng số đường dẫn: tiền tố các khu vực web và nhóm API JSON.
 */
public final class ApiConstants {

    public static final String HOME_PATH = "/";

    /** Nhóm API JSON (chatbot, theo dõi, bình luận, thông báo, tải ảnh chương). */
    public static final String API_ROOT = "/api";
    public static final String API_STUDIO_PATH = API_ROOT + "/studio";
    public static final String API_STORIES_PATH = API_ROOT + "/stories";
    public static final String API_COMMENTS_PATH = API_ROOT + "/comments";
    public static final String API_NOTIFICATIONS_PATH = API_ROOT + "/notifications";
    public static final String API_STUDIO_CHAPTERS_PATH = API_STUDIO_PATH + "/chapters";
    public static final String API_STUDIO_STATS_PATH = API_STUDIO_PATH + "/stats";

    /** Phần công khai: danh mục truyện, thể loại, bảng xếp hạng. */
    public static final String STORIES_PATH = "/stories";
    public static final String GENRES_PATH = "/genres";
    public static final String RANKINGS_PATH = "/rankings";

    /** Các khu vực web cần đăng nhập, phân theo vai trò. */
    public static final String ME_ROOT = "/me";
    public static final String STUDIO_ROOT = "/studio";
    public static final String ADMIN_ROOT = "/admin";

    public static final String LOGIN_PATH = "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String PROFILE_PATH = ME_ROOT + "/profile";
    public static final String PASSWORD_PATH = ME_ROOT + "/password";
    public static final String LIBRARY_PATH = ME_ROOT + "/library";
    public static final String HISTORY_PATH = ME_ROOT + "/history";
    public static final String NOTIFICATIONS_PATH = ME_ROOT + "/notifications";
    public static final String AUTHOR_REQUEST_PATH = ME_ROOT + "/author-request";
    public static final String STUDIO_STORIES_PATH = STUDIO_ROOT + "/stories";
    public static final String STUDIO_CHAPTERS_PATH = STUDIO_ROOT + "/chapters";
    public static final String STUDIO_STATS_PATH = STUDIO_ROOT + "/stats";
    public static final String ADMIN_USERS_PATH = ADMIN_ROOT + "/users";
    public static final String ADMIN_GENRES_PATH = ADMIN_ROOT + "/genres";
    public static final String ADMIN_AUTHOR_REQUESTS_PATH = ADMIN_ROOT + "/author-requests";

    public static final String FORBIDDEN_PAGE_PATH = "/error/403";
    public static final String HEALTH_PATH = "/actuator/health";

    /**
     * Tài nguyên tĩnh và ảnh truyện. Một trang đọc tải hàng chục ảnh, nên các filter chạy theo từng request
     * (ghi access log, làm mới quyền trong phiên) bỏ qua nhóm đường dẫn này.
     */
    public static final List<String> STATIC_RESOURCE_PREFIXES =
            List.of("/webjars/", "/css/", "/js/", "/images/", "/media/", "/favicon.ico");

    /** Số dòng mỗi trang ở các bảng quản trị. */
    public static final int ADMIN_PAGE_SIZE = 20;

    /** Số thẻ truyện mỗi trang danh sách: chia hết cho 2, 3, 4 và 6 cột của lưới ở mọi cỡ màn hình. */
    public static final int STORY_PAGE_SIZE = 24;

    /** Số thẻ truyện ở mỗi khối của trang chủ. */
    public static final int HOME_SECTION_SIZE = 12;

    public static final int RANKING_SIZE = 20;
    public static final int COMMENT_PAGE_SIZE = 20;

    /** Số truyện gần nhất hiện ở trang lịch sử đọc và khối "Đọc tiếp". */
    public static final int HISTORY_SIZE = 48;
    public static final int CONTINUE_READING_SIZE = 6;

    public static final int NOTIFICATION_PAGE_SIZE = 20;

    /** Số chương trong bảng "chương được xem nhiều nhất" của tác giả. */
    public static final int STUDIO_TOP_CHAPTER_SIZE = 10;

    /** Số ngày (tính cả hôm nay) trên biểu đồ lượt xem của tác giả. */
    public static final int STATS_CHART_DAYS = 30;

    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
