package vn.edu.utc.comic.chapter.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.dto.ChapterSummaryResponse;
import vn.edu.utc.comic.chapter.entity.Chapter;

/** Truy vấn chương. */
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    Optional<Chapter> findByStoryIdAndChapterNo(Long storyId, int chapterNo);

    /** Danh sách chương đã đăng của một truyện, mới nhất trước; chỉ lấy các cột hiển thị. */
    @Query("""
            SELECT new vn.edu.utc.comic.chapter.dto.ChapterSummaryResponse(
                c.chapterNo, c.title, c.publishedAt, c.viewCount)
            FROM Chapter c
            WHERE c.story.id = :storyId AND c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED
            ORDER BY c.chapterNo DESC
            """)
    List<ChapterSummaryResponse> findPublishedSummaries(@Param("storyId") Long storyId);

    /** Số của chương đã đăng gần nhất đứng TRƯỚC chương cho trước; số chương có thể không liên tiếp. */
    @Query("""
            SELECT MAX(c.chapterNo) FROM Chapter c
            WHERE c.story.id = :storyId AND c.chapterNo < :chapterNo
              AND c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED
            """)
    Optional<Integer> findPreviousPublishedNo(@Param("storyId") Long storyId, @Param("chapterNo") int chapterNo);

    /** Số của chương đã đăng gần nhất đứng SAU chương cho trước. */
    @Query("""
            SELECT MIN(c.chapterNo) FROM Chapter c
            WHERE c.story.id = :storyId AND c.chapterNo > :chapterNo
              AND c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED
            """)
    Optional<Integer> findNextPublishedNo(@Param("storyId") Long storyId, @Param("chapterNo") int chapterNo);

    /** Tự mở transaction riêng: xem lý do ở {@code ViewCountService}. */
    @Transactional
    @Modifying
    @Query("UPDATE Chapter c SET c.viewCount = c.viewCount + 1 WHERE c.id = :chapterId")
    int incrementViewCount(@Param("chapterId") Long chapterId);
}
