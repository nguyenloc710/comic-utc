package vn.edu.utc.comic.interaction.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.interaction.dto.ReadingHistoryRow;
import vn.edu.utc.comic.interaction.entity.ReadingHistory;
import vn.edu.utc.comic.interaction.entity.UserStoryId;

/** Truy vấn tiến độ đọc. */
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, UserStoryId> {

    /**
     * Ghi chương đang đọc của một người ở một truyện: thêm dòng nếu chưa có, ghi đè nếu đã có.
     * Phải là SQL gốc vì JPQL không có upsert; đọc-rồi-ghi thì hai tab mở cùng lúc sẽ đụng khóa chính.
     */
    @Modifying
    @Query(value = """
            INSERT INTO reading_history (user_id, story_id, chapter_id, updated_at)
            VALUES (:userId, :storyId, :chapterId, :updatedAt) AS incoming
            ON DUPLICATE KEY UPDATE chapter_id = incoming.chapter_id, updated_at = incoming.updated_at
            """, nativeQuery = true)
    int saveProgress(@Param("userId") Long userId, @Param("storyId") Long storyId,
                     @Param("chapterId") Long chapterId, @Param("updatedAt") Instant updatedAt);

    /** Số của chương đang đọc dở, nếu chương đó vẫn còn công khai. */
    @Query("""
            SELECT c.chapterNo FROM ReadingHistory h JOIN Chapter c ON c.id = h.chapterId
            WHERE h.id.userId = :userId AND h.id.storyId = :storyId
              AND c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED
            """)
    Optional<Integer> findCurrentChapterNo(@Param("userId") Long userId, @Param("storyId") Long storyId);

    /** Các truyện (còn công khai) một người đọc gần đây nhất, kèm chương đang đọc dở. */
    @Query("""
            SELECT new vn.edu.utc.comic.interaction.dto.ReadingHistoryRow(s, c.chapterNo, h.updatedAt)
            FROM ReadingHistory h JOIN Story s ON s.id = h.id.storyId JOIN Chapter c ON c.id = h.chapterId
            WHERE h.id.userId = :userId
              AND s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED AND s.deletedAt IS NULL
              AND c.status = vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED
            ORDER BY h.updatedAt DESC
            """)
    List<ReadingHistoryRow> findRecent(@Param("userId") Long userId, Pageable pageable);
}
