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
    public static final String STORY_LIST = "story/list";
    public static final String STORY_DETAIL = "story/detail";
    public static final String STORY_RANKINGS = "story/rankings";
    public static final String CHAPTER_READ = "chapter/read";

    // ----- Xác thực & tài khoản -----
    public static final String AUTH_LOGIN = "auth/login";
    public static final String AUTH_REGISTER = "auth/register";
    public static final String ME_PROFILE = "me/profile";
    public static final String ME_PASSWORD = "me/password";
    public static final String ME_LIBRARY = "me/library";
    public static final String ME_HISTORY = "me/history";
    public static final String ME_NOTIFICATIONS = "me/notifications";
    public static final String ME_AUTHOR_REQUEST = "me/author-request";

    // ----- Tác giả -----
    public static final String STUDIO_DASHBOARD = "studio/dashboard";
    public static final String STUDIO_STORIES = "studio/stories";
    public static final String STUDIO_STORY_FORM = "studio/story-form";
    public static final String STUDIO_CHAPTERS = "studio/chapters";
    public static final String STUDIO_CHAPTER_COMIC_EDITOR = "studio/chapter-comic-editor";
    public static final String STUDIO_CHAPTER_NOVEL_EDITOR = "studio/chapter-novel-editor";
    public static final String STUDIO_STATS = "studio/stats";

    // ----- Quản trị -----
    public static final String ADMIN_DASHBOARD = "admin/dashboard";
    public static final String ADMIN_USERS = "admin/users";
    public static final String ADMIN_GENRES = "admin/genres";
    public static final String ADMIN_GENRE_FORM = "admin/genre-form";
    public static final String ADMIN_AUTHOR_REQUESTS = "admin/author-requests";
    public static final String ADMIN_AUTHOR_REQUEST_DETAIL = "admin/author-request-detail";
    public static final String ADMIN_STORIES = "admin/stories";
    public static final String ADMIN_STORY_DETAIL = "admin/story-detail";
    public static final String ADMIN_COMMENTS = "admin/comments";
    public static final String ADMIN_REPORTS = "admin/reports";
    public static final String ADMIN_REPORT_DETAIL = "admin/report-detail";
    public static final String ADMIN_SETTINGS = "admin/settings";
    public static final String ADMIN_AUDIT_LOGS = "admin/audit-logs";
    public static final String ADMIN_CHATBOT = "admin/chatbot";

    public static final String ERROR_PAGE = "error/error";

    // ----- Redirect -----
    private static final String REDIRECT = "redirect:";
    public static final String REDIRECT_HOME = REDIRECT + ApiConstants.HOME_PATH;
    public static final String REDIRECT_LOGIN = REDIRECT + ApiConstants.LOGIN_PATH;
    public static final String REDIRECT_PROFILE = REDIRECT + ApiConstants.PROFILE_PATH;
    public static final String REDIRECT_ADMIN_USERS = REDIRECT + ApiConstants.ADMIN_USERS_PATH;
    public static final String REDIRECT_ADMIN_GENRES = REDIRECT + ApiConstants.ADMIN_GENRES_PATH;
    public static final String REDIRECT_ADMIN_AUTHOR_REQUESTS = REDIRECT + ApiConstants.ADMIN_AUTHOR_REQUESTS_PATH;
    public static final String REDIRECT_ADMIN_COMMENTS = REDIRECT + ApiConstants.ADMIN_COMMENTS_PATH;
    public static final String REDIRECT_ADMIN_REPORTS = REDIRECT + ApiConstants.ADMIN_REPORTS_PATH;
    public static final String REDIRECT_ADMIN_SETTINGS = REDIRECT + ApiConstants.ADMIN_SETTINGS_PATH;
    public static final String REDIRECT_AUTHOR_REQUEST = REDIRECT + ApiConstants.AUTHOR_REQUEST_PATH;
    public static final String REDIRECT_NOTIFICATIONS = REDIRECT + ApiConstants.NOTIFICATIONS_PATH;
    public static final String REDIRECT_STUDIO_STORIES = REDIRECT + ApiConstants.STUDIO_STORIES_PATH;

    /** Redirect tới một đường dẫn chỉ biết lúc chạy (có id trên URL), ví dụ trang chương của một truyện. */
    public static String redirectTo(String path) {
        return REDIRECT + path;
    }

    private ViewConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
