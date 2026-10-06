package vn.edu.utc.comic.genre.dto;

/** Thể loại ở dạng nhãn: tên để hiển thị và slug để dựng link / làm giá trị bộ lọc. */
public record GenreTagResponse(String name, String slug) {
}
