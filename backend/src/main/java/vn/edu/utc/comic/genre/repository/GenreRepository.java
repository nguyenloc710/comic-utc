package vn.edu.utc.comic.genre.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.genre.entity.Genre;

/** Truy vấn thể loại. */
public interface GenreRepository extends JpaRepository<Genre, Long> {
}
