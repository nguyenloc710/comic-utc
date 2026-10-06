package vn.edu.utc.comic.common.constant;

/**
 * Tên template Thymeleaf, tên thuộc tính trong Model và các đường dẫn redirect.
 * Controller không viết chuỗi tên view trực tiếp.
 */
public final class ViewConstants {

    // ----- Thuộc tính Model dùng chung -----
    public static final String ATTR_FORM = "form";
    public static final String ATTR_PAGE = "page";
    public static final String ATTR_FILTER = "filter";
    public static final String ATTR_FLASH_SUCCESS = "flashSuccess";
    public static final String ATTR_FLASH_ERROR = "flashError";
    public static final String ATTR_ERROR_MESSAGE = "errorMessage";
    public static final String ATTR_ERROR_STATUS = "errorStatus";
    public static final String ATTR_CURRENT_USER = "currentUser";
    public static final String ATTR_CURRENT_USER_AVATAR_URL = "currentUserAvatarUrl";
    public static final String ATTR_DISPLAY_ZONE = "displayZone";
    public static final String ATTR_REQUEST_PATH = "requestPath";
    public static final String ATTR_FILTER_QUERY = "filterQuery";

    // ----- Công khai -----
    public static final String HOME = "home";

    // ----- Xác thực & tài khoản -----
    public static final String AUTH_LOGIN = "auth/login";
    public static final String AUTH_REGISTER = "auth/register";
    public static final String ME_PROFILE = "me/profile";
    public static final String ME_PASSWORD = "me/password";

    // ----- Tác giả -----
    public static final String STUDIO_DASHBOARD = "studio/dashboard";

    // ----- Quản trị -----
    public static final String ADMIN_DASHBOARD = "admin/dashboard";
    public static final String ADMIN_USERS = "admin/users";
    public static final String ADMIN_GENRES = "admin/genres";
    public static final String ADMIN_GENRE_FORM = "admin/genre-form";

    public static final String ERROR_PAGE = "error/error";

    // ----- Redirect -----
    private static final String REDIRECT = "redirect:";
    public static final String REDIRECT_HOME = REDIRECT + ApiConstants.HOME_PATH;
    public static final String REDIRECT_LOGIN = REDIRECT + ApiConstants.LOGIN_PATH;
    public static final String REDIRECT_PROFILE = REDIRECT + ApiConstants.PROFILE_PATH;
    public static final String REDIRECT_ADMIN_USERS = REDIRECT + ApiConstants.ADMIN_USERS_PATH;
    public static final String REDIRECT_ADMIN_GENRES = REDIRECT + ApiConstants.ADMIN_GENRES_PATH;

    private ViewConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
