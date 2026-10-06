package vn.edu.utc.comic.common.constant;

import java.util.Map;

/**
 * Hằng số lưu trữ ảnh.
 */
public final class StorageConstants {

    /** Tiền tố URL công khai của ảnh lưu trên đĩa máy chủ. */
    public static final String MEDIA_URL_PREFIX = "/media/";

    /** Thư mục ảnh đại diện; ảnh của mỗi tài khoản nằm ở avatars/{userId}. */
    public static final String AVATAR_DIRECTORY = "avatars";

    /**
     * Định dạng ảnh được nhận: tên định dạng do ImageIO nhận diện từ NỘI DUNG tệp → đuôi dùng khi lưu.
     * Đuôi và MIME do trình duyệt gửi lên không được tin.
     */
    public static final Map<String, String> EXTENSION_BY_IMAGE_FORMAT = Map.of(
            "jpeg", "jpg",
            "png", "png",
            "webp", "webp");

    /** Tên các định dạng được nhận, điền vào thông báo lỗi. */
    public static final String ALLOWED_IMAGE_FORMATS_LABEL = "JPG, PNG, WebP";

    /** Dùng khi bảng setting chưa có khóa tương ứng. */
    public static final int DEFAULT_IMAGE_MAX_SIZE_MB = 5;

    public static final long BYTES_PER_MB = 1024L * 1024L;

    private StorageConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
