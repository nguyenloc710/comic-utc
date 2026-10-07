package vn.edu.utc.comic.chapter.dto;

/**
 * Một trang ảnh trong trình soạn chương truyện tranh; khác bản phía người đọc ở chỗ có id để sắp xếp và xóa.
 */
public record StudioChapterPageResponse(Long id, int pageNo, String imageUrl, int width, int height) {
}
