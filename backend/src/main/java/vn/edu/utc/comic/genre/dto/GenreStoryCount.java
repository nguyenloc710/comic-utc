package vn.edu.utc.comic.genre.dto;

/** Kết quả truy vấn đếm số truyện theo thể loại. */
public record GenreStoryCount(Long genreId, long storyCount) {
}
