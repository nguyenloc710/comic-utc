package vn.edu.utc.comic.chapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.chapter.entity.ChapterContent;

/** Truy vấn nội dung chương truyện chữ. */
public interface ChapterContentRepository extends JpaRepository<ChapterContent, Long> {
}
