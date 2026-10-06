package vn.edu.utc.comic.common.setting;

/**
 * Khóa của các tham số vận hành trong bảng setting (danh sách đầy đủ: docs/01 §8).
 * Khóa chỉ được khai báo ở đây khi đã có mã nguồn đọc nó.
 */
public final class SettingKeys {

    public static final String UPLOAD_IMAGE_MAX_SIZE_MB = "upload.image.max_size_mb";

    private SettingKeys() {
        throw new UnsupportedOperationException("Utility class");
    }
}
