package vn.edu.utc.comic.common.constant;

/**
 * Hằng số đường dẫn: tiền tố các khu vực web và nhóm API JSON.
 */
public final class ApiConstants {

    public static final String HOME_PATH = "/";

    /** Nhóm API JSON (chatbot, theo dõi, bình luận, thông báo, tải ảnh chương). */
    public static final String API_ROOT = "/api";
    public static final String API_STUDIO_PATH = API_ROOT + "/studio";

    /** Các khu vực web cần đăng nhập, phân theo vai trò. */
    public static final String ME_ROOT = "/me";
    public static final String STUDIO_ROOT = "/studio";
    public static final String ADMIN_ROOT = "/admin";

    public static final String LOGIN_PATH = "/login";
    public static final String FORBIDDEN_PAGE_PATH = "/error/403";
    public static final String HEALTH_PATH = "/actuator/health";

    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
