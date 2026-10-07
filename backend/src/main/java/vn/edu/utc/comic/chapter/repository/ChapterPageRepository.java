package vn.edu.utc.comic.chapter.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.chapter.entity.ChapterPage;

/** Truy vấn trang ảnh của chương truyện tranh. */
public interface ChapterPageRepository extends JpaRepository<ChapterPage, Long> {

    List<ChapterPage> findByChapterIdOrderByPageNoAsc(Long chapterId);

    int countByChapterId(Long chapterId);

    /** Chỉ lấy khóa ảnh, không nạp thực thể: dùng khi sắp xóa cả chương và cần biết tệp nào phải dọn. */
    @Query("SELECT p.imagePath FROM ChapterPage p WHERE p.chapter.id = :chapterId")
    List<String> findImagePathsByChapterId(@Param("chapterId") Long chapterId);

    /** Xóa mọi trang của một chương bằng một câu lệnh (khi xóa chương nháp). */
    @Modifying
    @Query("DELETE FROM ChapterPage p WHERE p.chapter.id = :chapterId")
    int deleteByChapterId(@Param("chapterId") Long chapterId);
}
