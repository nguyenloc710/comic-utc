package vn.edu.utc.comic.chapter.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.dto.ChapterSummaryResponse;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.stats.dto.TopChapterResponse;

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

    /** Chương kèm truyện của nó trong một truy vấn, cho các thao tác cần kiểm tra chủ truyện. */
    @Query("SELECT c FROM Chapter c JOIN FETCH c.story WHERE c.id = :chapterId")
    Optional<Chapter> findWithStoryById(@Param("chapterId") Long chapterId);

    /** Mọi chương của một truyện (mọi trạng thái), cho khu vực tác giả. */
    List<Chapter> findByStoryIdOrderByChapterNoDesc(Long storyId);

    boolean existsByStoryId(Long storyId);

    boolean existsByStoryIdAndChapterNo(Long storyId, int chapterNo);

    @Query("SELECT MAX(c.chapterNo) FROM Chapter c WHERE c.story.id = :storyId")
    Optional<Integer> findMaxChapterNo(@Param("storyId") Long storyId);

    /** Id các chương hẹn giờ đã tới hạn, chương hẹn sớm hơn đứng trước. */
    @Query("""
            SELECT c.id FROM Chapter c
            WHERE c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.SCHEDULED AND c.scheduledAt <= :now
            ORDER BY c.scheduledAt ASC, c.id ASC
            """)
    List<Long> findDueScheduledIds(@Param("now") Instant now, Pageable pageable);

    /**
     * Đăng một chương hẹn giờ đã tới hạn bằng câu UPDATE CÓ ĐIỀU KIỆN: trả về 1 nếu chính lời gọi này đổi được
     * trạng thái, 0 nếu chương không còn ở trạng thái hẹn giờ (đã được đăng bởi lượt chạy khác, hoặc tác giả vừa
     * hủy hẹn). VERSIONED để câu lệnh tăng cột version — nếu tác giả đang lưu form của chính chương này bằng dữ
     * liệu đọc từ trước, lần lưu đó sẽ bị từ chối thay vì ghi đè trạng thái vừa đổi.
     */
    @Modifying
    @Query("""
            UPDATE VERSIONED Chapter c
            SET c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED, c.publishedAt = :now,
                c.scheduledAt = NULL
            WHERE c.id = :chapterId
              AND c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.SCHEDULED AND c.scheduledAt <= :now
            """)
    int publishScheduled(@Param("chapterId") Long chapterId, @Param("now") Instant now);

    /**
     * Khóa dòng chương đến hết transaction, để các thao tác trên trang ảnh của cùng một chương chạy lần lượt.
     * Chỉ khóa dòng chương (không nối sang truyện) để không đảo thứ tự khóa truyện → chương của các thao tác khác.
     */
    @Query(value = "SELECT id FROM chapter WHERE id = :chapterId FOR UPDATE", nativeQuery = true)
    Optional<Long> lockForPageUpdate(@Param("chapterId") Long chapterId);

    @Modifying
    @Query("UPDATE Chapter c SET c.pageCount = :pageCount WHERE c.id = :chapterId")
    int updatePageCount(@Param("chapterId") Long chapterId, @Param("pageCount") int pageCount);

    /** Các chương đã đăng được xem nhiều nhất trên mọi truyện chưa xóa của một tác giả. */
    @Query("""
            SELECT new vn.edu.utc.comic.stats.dto.TopChapterResponse(
                s.title, s.slug, c.chapterNo, c.title, c.viewCount)
            FROM Chapter c JOIN c.story s
            WHERE s.author.id = :authorId AND s.deletedAt IS NULL
              AND c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED
            ORDER BY c.viewCount DESC, c.id DESC
            """)
    List<TopChapterResponse> findTopChaptersByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    /** Số chương người đọc đang thấy được (chương đã đăng của truyện đang công khai). */
    @Query("""
            SELECT COUNT(c) FROM Chapter c JOIN c.story s
            WHERE c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED
              AND s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED AND s.deletedAt IS NULL
            """)
    long countPubliclyReadable();

    /** Chỉ id truyện của chương, để khóa dòng truyện trước khi nạp gì khác. */
    @Query("SELECT c.story.id FROM Chapter c WHERE c.id = :chapterId")
    Optional<Long> findStoryIdById(@Param("chapterId") Long chapterId);
}
