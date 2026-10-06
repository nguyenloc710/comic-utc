package vn.edu.utc.comic.common.constant;

/**
 * Tên template Thymeleaf, tên thuộc tính trong Model và các đường dẫn redirect.
 * Controller không viết chuỗi tên view trực tiếp.
 */
public final class ViewConstants {

    // ----- Thuộc tính Model dùng chung -----
    public static final String ATTR_FLASH_SUCCESS = "flashSuccess";
    public static final String ATTR_FLASH_ERROR = "flashError";
    public static final String ATTR_ERROR_MESSAGE = "errorMessage";
    public static final String ATTR_ERROR_STATUS = "errorStatus";
    public static final String ATTR_CURRENT_USER = "currentUser";
    public static final String ATTR_DISPLAY_ZONE = "displayZone";
    public static final String ATTR_REQUEST_PATH = "requestPath";
    public static final String ATTR_FILTER_QUERY = "filterQuery";

    // ----- Công khai -----
    public static final String HOME = "home";

    // ----- Xác thực -----
    public static final String AUTH_LOGIN = "auth/login";

    // ----- Tác giả -----
    public static final String STUDIO_DASHBOARD = "studio/dashboard";

    // ----- Quản trị -----
    public static final String ADMIN_DASHBOARD = "admin/dashboard";

    public static final String ERROR_PAGE = "error/error";

    // ----- Redirect -----
    private static final String REDIRECT = "redirect:";
    public static final String REDIRECT_HOME = REDIRECT + ApiConstants.HOME_PATH;

    private ViewConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
