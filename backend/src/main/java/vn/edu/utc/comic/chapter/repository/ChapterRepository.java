package vn.edu.utc.comic.chapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.chapter.entity.Chapter;

/** Truy vấn chương. */
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
}
