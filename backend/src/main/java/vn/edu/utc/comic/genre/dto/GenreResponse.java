package vn.edu.utc.comic.genre.dto;

/**
 * Một thể loại ở trang quản trị.
 *
 * @param storyCount số truyện đang gắn thể loại này (kể cả truyện nháp và đã xóa mềm);
 *                   lớn hơn 0 thì thể loại không xóa được, chỉ tắt được
 */
public record GenreResponse(
        Long id,
        String name,
        String slug,
        String description,
        int sortOrder,
        boolean active,
        long storyCount) {
}
