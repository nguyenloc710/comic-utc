package vn.edu.utc.comic.chapter.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.chapter.entity.ChapterPage;

/** Truy vấn trang ảnh của chương truyện tranh. */
public interface ChapterPageRepository extends JpaRepository<ChapterPage, Long> {

    List<ChapterPage> findByChapterIdOrderByPageNoAsc(Long chapterId);
}
