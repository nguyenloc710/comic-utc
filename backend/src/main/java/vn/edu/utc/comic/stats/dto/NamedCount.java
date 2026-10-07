package vn.edu.utc.comic.stats.dto;

/** Một cột trên biểu đồ phân bố: tên (thể loại, truyện) và con số của nó. */
public record NamedCount(String name, Long count) {
}
