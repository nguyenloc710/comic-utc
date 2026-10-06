package vn.edu.utc.comic.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Cửa DUY NHẤT để lưu, xóa và dựng URL ảnh (bìa truyện, trang truyện tranh, avatar).
 *
 * <p>Cơ sở dữ liệu chỉ giữ khóa do {@link #storeImage} trả về; template nhận URL đã dựng qua
 * {@link #resolveUrl}. Nhờ vậy đổi nơi lưu (đĩa cục bộ → dịch vụ đám mây) chỉ là thêm một lớp cài đặt,
 * không phải sửa dữ liệu hay template.
 */
public interface StorageService {

    /**
     * Kiểm tra rồi lưu một ảnh.
     *
     * @param file      tệp người dùng tải lên
     * @param directory thư mục tương đối do service nghiệp vụ quyết định, ví dụ {@code stories/12/cover}
     * @return khóa đã lưu kèm kích thước ảnh
     * @throws vn.edu.utc.comic.common.exception.ApiException IMAGE_TYPE_NOT_ALLOWED, IMAGE_TOO_LARGE
     *                                                        hoặc STORAGE_FAILED
     */
    StoredFile storeImage(MultipartFile file, String directory);

    /** Xóa ảnh theo khóa. Tệp không còn tồn tại không phải là lỗi. */
    void delete(String key);

    /** URL để trình duyệt tải ảnh; trả {@code null} khi chưa có ảnh (khóa rỗng). */
    String resolveUrl(String key);
}
