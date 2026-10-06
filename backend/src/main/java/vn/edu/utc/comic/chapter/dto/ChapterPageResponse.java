package vn.edu.utc.comic.chapter.dto;

/**
 * Một trang ảnh của chương truyện tranh.
 *
 * @param width  chiều rộng gốc, để trình duyệt giữ sẵn chỗ đúng tỉ lệ trước khi ảnh tải xong
 * @param height chiều cao gốc
 */
public record ChapterPageResponse(int pageNo, String imageUrl, int width, int height) {
}
