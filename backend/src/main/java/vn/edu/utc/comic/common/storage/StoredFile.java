package vn.edu.utc.comic.common.storage;

/**
 * Kết quả lưu một ảnh.
 *
 * @param key       khóa tương đối so với thư mục gốc, là giá trị ghi vào các cột {@code *_path}
 * @param width     chiều rộng ảnh (pixel)
 * @param height    chiều cao ảnh (pixel)
 * @param sizeBytes dung lượng tệp
 */
public record StoredFile(String key, int width, int height, long sizeBytes) {
}
