package vn.edu.utc.comic.genre.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.genre.dto.GenreStoryCount;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.stats.dto.NamedCount;

/** Truy vấn thể loại. */
public interface GenreRepository extends JpaRepository<Genre, Long> {

    List<Genre> findAllByOrderBySortOrderAscNameAsc();

    List<Genre> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<Genre> findBySlugAndActiveTrue(String slug);

    /** So sánh không phân biệt hoa thường và dấu nhờ collation utf8mb4_unicode_ci của cột. */
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    /** Số truyện theo từng thể loại trong một truy vấn; thể loại chưa có truyện nào không có dòng kết quả. */
    @Query("""
            SELECT new vn.edu.utc.comic.genre.dto.GenreStoryCount(g.id, COUNT(s))
            FROM Story s JOIN s.genres g
            GROUP BY g.id
            """)
    List<GenreStoryCount> countStoriesByGenre();

    /** Đếm cả truyện đã xóa mềm: dòng story_genre của chúng vẫn còn nên thể loại chưa xóa cứng được. */
    @Query("SELECT COUNT(s) FROM Story s JOIN s.genres g WHERE g.id = :genreId")
    long countStoriesUsing(@Param("genreId") Long genreId);

    /** Số truyện đang công khai của từng thể loại, nhiều nhất trước, cho biểu đồ phân bố thể loại. */
    @Query("""
            SELECT new vn.edu.utc.comic.stats.dto.NamedCount(g.name, COUNT(s))
            FROM Story s JOIN s.genres g
            WHERE s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED AND s.deletedAt IS NULL
            GROUP BY g.id, g.name
            ORDER BY COUNT(s) DESC, g.name ASC
            """)
    List<NamedCount> countPublicStoriesByGenre(Pageable pageable);
}
