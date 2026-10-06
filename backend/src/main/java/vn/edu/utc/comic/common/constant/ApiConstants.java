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

    /** Các khu vực web cần đăng nhập, phân theo vai trò. */
    public static final String ME_ROOT = "/me";
    public static final String STUDIO_ROOT = "/studio";
    public static final String ADMIN_ROOT = "/admin";

    public static final String LOGIN_PATH = "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String PROFILE_PATH = ME_ROOT + "/profile";
    public static final String PASSWORD_PATH = ME_ROOT + "/password";
    public static final String ADMIN_USERS_PATH = ADMIN_ROOT + "/users";
    public static final String ADMIN_GENRES_PATH = ADMIN_ROOT + "/genres";

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

    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
