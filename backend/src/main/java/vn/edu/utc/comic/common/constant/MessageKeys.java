package vn.edu.utc.comic.common.constant;

/**
 * Khóa của các thông báo trong i18n/messages.properties. Không viết chuỗi khóa trực tiếp trong service.
 */
public final class MessageKeys {

    // ----- Lỗi chung -----
    public static final String ERROR_VALIDATION = "error.validation";
    public static final String ERROR_RESOURCE_NOT_FOUND = "error.resource.not.found";
    public static final String ERROR_INTERNAL = "error.internal";
    public static final String ERROR_UNAUTHORIZED = "error.unauthorized";
    public static final String ERROR_FORBIDDEN = "error.forbidden";

    // ----- Ảnh & lưu trữ -----
    /** {0}: danh sách định dạng được nhận. */
    public static final String ERROR_IMAGE_TYPE_NOT_ALLOWED = "error.image.type.not.allowed";
    /** {0}: dung lượng tối đa tính bằng MB. */
    public static final String ERROR_IMAGE_TOO_LARGE = "error.image.too.large";
    public static final String ERROR_UPLOAD_TOO_LARGE = "error.upload.too.large";
    public static final String ERROR_STORAGE_FAILED = "error.storage.failed";

    // ----- Đăng nhập -----
    public static final String LOGIN_ERROR_BAD_CREDENTIALS = "login.error.bad.credentials";
    public static final String LOGIN_ERROR_LOCKED = "login.error.locked";
    public static final String LOGIN_ERROR_BANNED = "login.error.banned";

    // ----- Tài khoản -----
    public static final String ERROR_USER_NOT_FOUND = "error.user.not.found";
    public static final String ERROR_USER_SELF_BAN = "error.user.self.ban";
    public static final String ERROR_USERNAME_DUPLICATED = "error.username.duplicated";
    public static final String ERROR_EMAIL_DUPLICATED = "error.email.duplicated";
    /** Hai người đăng ký cùng tên đăng nhập hoặc email đúng cùng lúc; cơ sở dữ liệu chặn người đến sau. */
    public static final String ERROR_REGISTRATION_CONFLICT = "error.registration.conflict";
    /** {0}: độ dài tối thiểu. */
    public static final String ERROR_PASSWORD_WEAK = "error.password.weak";
    public static final String ERROR_PASSWORD_CONFIRM_MISMATCH = "error.password.confirm.mismatch";
    public static final String ERROR_PASSWORD_CURRENT_WRONG = "error.password.current.wrong";
    public static final String ERROR_PASSWORD_SAME_AS_CURRENT = "error.password.same.as.current";

    // ----- Thể loại -----
    public static final String ERROR_GENRE_NOT_FOUND = "error.genre.not.found";
    /** {0}: số truyện đang dùng thể loại. */
    public static final String ERROR_GENRE_IN_USE = "error.genre.in.use";
    public static final String ERROR_GENRE_NAME_DUPLICATED = "error.genre.name.duplicated";
    public static final String ERROR_GENRE_NAME_INVALID = "error.genre.name.invalid";

    // ----- Truyện & chương -----
    public static final String ERROR_STORY_NOT_FOUND = "error.story.not.found";
    public static final String ERROR_CHAPTER_NOT_FOUND = "error.chapter.not.found";

    // ----- Tương tác của độc giả -----
    public static final String ERROR_RATING_OWN_STORY = "error.rating.own.story";
    public static final String ERROR_COMMENT_NOT_FOUND = "error.comment.not.found";
    /** {0}: số giây phải chờ giữa hai bình luận. */
    public static final String ERROR_COMMENT_TOO_FAST = "error.comment.too.fast";
    public static final String ERROR_COMMENT_PARENT_INVALID = "error.comment.parent.invalid";

    // ----- Flash -----
    /** {0}: tên hiển thị của người vừa đăng ký. */
    public static final String FLASH_REGISTERED = "flash.registered";
    public static final String FLASH_PROFILE_SAVED = "flash.profile.saved";
    public static final String FLASH_PASSWORD_CHANGED = "flash.password.changed";
    /** {0}: tên đăng nhập. */
    public static final String FLASH_USER_BANNED = "flash.user.banned";
    /** {0}: tên đăng nhập. */
    public static final String FLASH_USER_UNBANNED = "flash.user.unbanned";
    /** {0}: tên thể loại. */
    public static final String FLASH_GENRE_CREATED = "flash.genre.created";
    public static final String FLASH_GENRE_UPDATED = "flash.genre.updated";
    public static final String FLASH_GENRE_DELETED = "flash.genre.deleted";

    private MessageKeys() {
        throw new UnsupportedOperationException("Utility class");
    }
}
