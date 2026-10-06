package vn.edu.utc.comic.common.storage;

/**
 * Thông tin đọc được từ nội dung một ảnh hợp lệ.
 *
 * @param extension đuôi tệp suy ra từ định dạng thật của ảnh (không lấy từ tên tệp người dùng gửi)
 * @param width     chiều rộng (pixel)
 * @param height    chiều cao (pixel)
 */
public record ImageInfo(String extension, int width, int height) {
}
