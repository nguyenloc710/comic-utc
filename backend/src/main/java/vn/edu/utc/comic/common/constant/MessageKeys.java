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

    public static final String FLASH_AUTHOR_REQUEST_SUBMITTED = "flash.author.request.submitted";
    /** {0}: bút danh vừa được cấp. */
    public static final String FLASH_AUTHOR_REQUEST_APPROVED = "flash.author.request.approved";
    public static final String FLASH_AUTHOR_REQUEST_REJECTED = "flash.author.request.rejected";
    public static final String FLASH_NOTIFICATIONS_READ = "flash.notifications.read";
    public static final String FLASH_STORY_CREATED = "flash.story.created";
    public static final String FLASH_STORY_UPDATED = "flash.story.updated";
    /** {0}: tên truyện. */
    public static final String FLASH_STORY_PUBLISHED = "flash.story.published";
    /** {0}: tên truyện. */
    public static final String FLASH_STORY_DELETED = "flash.story.deleted";
    public static final String FLASH_CHAPTER_SAVED = "flash.chapter.saved";
    public static final String FLASH_CHAPTER_PUBLISHED = "flash.chapter.published";
    public static final String FLASH_CHAPTER_SCHEDULED = "flash.chapter.scheduled";
    public static final String FLASH_CHAPTER_UNSCHEDULED = "flash.chapter.unscheduled";
    /** {0}: số chương. */
    public static final String FLASH_CHAPTER_DELETED = "flash.chapter.deleted";

    // ----- Đăng ký tác giả & thông báo -----
    public static final String ERROR_AUTHOR_REQUEST_NOT_FOUND = "error.author.request.not.found";
    public static final String ERROR_AUTHOR_REQUEST_NOT_ALLOWED = "error.author.request.not.allowed";
    public static final String ERROR_AUTHOR_REQUEST_ALREADY_PENDING = "error.author.request.already.pending";
    /** {0}: số ngày phải chờ sau khi bị từ chối. */
    public static final String ERROR_AUTHOR_REQUEST_COOLDOWN = "error.author.request.cooldown";
    public static final String ERROR_AUTHOR_REQUEST_ALREADY_REVIEWED = "error.author.request.already.reviewed";
    /** {0}: bút danh. */
    public static final String ERROR_AUTHOR_PEN_NAME_TAKEN = "error.author.pen.name.taken";
    public static final String ERROR_PEN_NAME_DUPLICATED = "error.pen.name.duplicated";
    public static final String ERROR_NOTIFICATION_NOT_FOUND = "error.notification.not.found";
    /** Tiền tố của câu thông báo: khóa đầy đủ là {@code notification.<NotificationType>}. */
    public static final String NOTIFICATION_PREFIX = "notification.";

    // ----- Quản lý truyện & chương của tác giả -----
    public static final String ERROR_CONCURRENT_UPDATE = "error.concurrent.update";
    public static final String ERROR_STORY_NOT_READY_TO_PUBLISH = "error.story.not.ready.to.publish";
    public static final String ERROR_STORY_VISIBILITY_NOT_CHANGEABLE = "error.story.visibility.not.changeable";
    public static final String ERROR_STORY_TITLE_INVALID = "error.story.title.invalid";
    public static final String ERROR_STORY_TYPE_LOCKED = "error.story.type.locked";
    public static final String ERROR_STORY_GENRE_INVALID = "error.story.genre.invalid";
    public static final String ERROR_CHAPTER_INVALID_TRANSITION = "error.chapter.invalid.transition";
    public static final String ERROR_CHAPTER_EMPTY = "error.chapter.empty";
    public static final String ERROR_CHAPTER_SCHEDULE_INVALID = "error.chapter.schedule.invalid";
    public static final String ERROR_CHAPTER_NOT_DELETABLE = "error.chapter.not.deletable";
    /** {0}: số ảnh tối đa mỗi chương. */
    public static final String ERROR_CHAPTER_PAGE_LIMIT = "error.chapter.page.limit";
    public static final String ERROR_CHAPTER_PAGE_NOT_FOUND = "error.chapter.page.not.found";
    public static final String ERROR_CHAPTER_PAGE_ORDER_INVALID = "error.chapter.page.order.invalid";
    public static final String ERROR_CHAPTER_TYPE_MISMATCH = "error.chapter.type.mismatch";
    public static final String ERROR_CHAPTER_NO_DUPLICATED = "error.chapter.no.duplicated";
    public static final String ERROR_CHAPTER_NO_LOCKED = "error.chapter.no.locked";
    public static final String ERROR_CHAPTER_CONTENT_REQUIRED = "error.chapter.content.required";
    // ----- Kiểm duyệt, báo cáo, hệ thống -----
    /** {0}: tên truyện. */
    public static final String FLASH_STORY_HIDDEN = "flash.story.hidden";
    /** {0}: tên truyện. */
    public static final String FLASH_STORY_UNHIDDEN = "flash.story.unhidden";
    public static final String FLASH_CHAPTER_HIDDEN = "flash.chapter.hidden";
    public static final String FLASH_CHAPTER_UNHIDDEN = "flash.chapter.unhidden";
    public static final String FLASH_COMMENT_HIDDEN = "flash.comment.hidden";
    public static final String FLASH_COMMENT_UNHIDDEN = "flash.comment.unhidden";
    public static final String FLASH_REPORT_DISMISSED = "flash.report.dismissed";
    public static final String FLASH_REPORT_RESOLVED = "flash.report.resolved";
    /** {0}: tên đăng nhập, {1}: vai trò mới. */
    public static final String FLASH_USER_ROLE_CHANGED = "flash.user.role.changed";
    /** {0}: khóa tham số. */
    public static final String FLASH_SETTING_SAVED = "flash.setting.saved";
    public static final String ERROR_STORY_MODERATION_INVALID = "error.story.moderation.invalid";
    public static final String ERROR_COMMENT_MODERATION_INVALID = "error.comment.moderation.invalid";
    public static final String ERROR_REASON_REQUIRED = "error.reason.required";
    public static final String ERROR_REPORT_NOT_FOUND = "error.report.not.found";
    public static final String ERROR_REPORT_ALREADY_HANDLED = "error.report.already.handled";
    public static final String ERROR_REPORT_ALREADY_PENDING = "error.report.already.pending";
    public static final String ERROR_REPORT_TARGET_NOT_FOUND = "error.report.target.not.found";
    public static final String ERROR_USER_LAST_ADMIN = "error.user.last.admin";
    public static final String ERROR_USER_SELF_ROLE_CHANGE = "error.user.self.role.change";
    /** {0}: khóa tham số. */
    public static final String ERROR_SETTING_NOT_FOUND = "error.setting.not.found";
    /** {0}: khóa tham số. */
    public static final String ERROR_SETTING_READ_ONLY = "error.setting.read.only";
    /** {0}: khóa tham số, {1}: kiểu giá trị. */
    public static final String ERROR_SETTING_VALUE_INVALID = "error.setting.value.invalid";
    private MessageKeys() {
        throw new UnsupportedOperationException("Utility class");
    }
}
