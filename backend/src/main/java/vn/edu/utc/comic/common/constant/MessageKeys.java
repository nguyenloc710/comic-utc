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

    private MessageKeys() {
        throw new UnsupportedOperationException("Utility class");
    }
}
