package vn.edu.utc.comic.story.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.stats.dto.AuthorStatsOverview;
import vn.edu.utc.comic.story.dto.StoryRatingSummary;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Truy vấn truyện; lọc động (tìm kiếm, thể loại, loại, trạng thái) đi qua Specification.
 *
 * <p>Các phương thức {@code add...Count} / {@code increment...} là cách DUY NHẤT để đổi bộ đếm của truyện:
 * cộng dồn ngay trong câu UPDATE nên hai request đồng thời không ghi đè nhau.
 */
public interface StoryRepository extends JpaRepository<Story, Long>, JpaSpecificationExecutor<Story> {

    Optional<Story> findBySlug(String slug);

    /** Truyện một người đang theo dõi (chỉ truyện còn công khai), mới theo dõi trước. */
    @Query(value = """
            SELECT s FROM Story s JOIN StoryFollow f ON f.id.storyId = s.id
            WHERE f.id.userId = :userId
              AND s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED AND s.deletedAt IS NULL
            ORDER BY f.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(s) FROM Story s JOIN StoryFollow f ON f.id.storyId = s.id
            WHERE f.id.userId = :userId
              AND s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED AND s.deletedAt IS NULL
            """)
    Page<Story> findFollowedBy(@Param("userId") Long userId, Pageable pageable);

    /** Rỗng khi không có truyện mang id này. */
    @Query("SELECT s.followCount FROM Story s WHERE s.id = :storyId")
    Optional<Integer> findFollowCount(@Param("storyId") Long storyId);

    @Query("""
            SELECT new vn.edu.utc.comic.story.dto.StoryRatingSummary(s.ratingSum, s.ratingCount)
            FROM Story s WHERE s.id = :storyId
            """)
    StoryRatingSummary findRatingSummary(@Param("storyId") Long storyId);

    /**
     * Khóa dòng truyện đến hết transaction. Gọi TRƯỚC khi thêm dòng con (theo dõi, đánh giá, bình luận) rồi
     * cộng bộ đếm: câu INSERT dòng con giữ khóa chia sẻ trên dòng truyện (kiểm tra khóa ngoại), nên hai người
     * cùng thao tác trên một truyện sẽ cùng giữ khóa chia sẻ rồi cùng chờ khóa ghi của câu UPDATE bộ đếm —
     * MySQL phải hủy một bên (deadlock). Lấy khóa ghi ngay từ đầu thì hai bên chỉ xếp hàng.
     *
     * <p>Phải là câu lệnh ĐẦU TIÊN của transaction. MySQL (REPEATABLE READ) chụp ảnh dữ liệu ở câu SELECT thường
     * đầu tiên; nếu đọc trước rồi mới khóa, các câu đọc sau khi chờ được khóa vẫn nhìn ảnh chụp cũ và không thấy
     * thay đổi mà bên giữ khóa trước vừa commit.
     *
     * @return id của truyện, rỗng nếu truyện không tồn tại
     */
    @Query(value = "SELECT id FROM story WHERE id = :storyId FOR UPDATE", nativeQuery = true)
    Optional<Long> lockForCounterUpdate(@Param("storyId") Long storyId);

    /** Tự mở transaction riêng: xem lý do ở {@code ViewCountService}. */
    @Transactional
    @Modifying
    @Query("UPDATE Story s SET s.viewCount = s.viewCount + 1 WHERE s.id = :storyId")
    int incrementViewCount(@Param("storyId") Long storyId);

    @Modifying
    @Query("UPDATE Story s SET s.followCount = s.followCount + :delta WHERE s.id = :storyId")
    int addFollowCount(@Param("storyId") Long storyId, @Param("delta") int delta);

    /**
     * @param sumDelta   chênh lệch tổng số sao (âm khi người dùng hạ điểm)
     * @param countDelta 1 khi là lượt đánh giá mới, 0 khi sửa lượt đã có
     */
    @Modifying
    @Query("""
            UPDATE Story s SET s.ratingSum = s.ratingSum + :sumDelta, s.ratingCount = s.ratingCount + :countDelta
            WHERE s.id = :storyId
            """)
    int addRating(@Param("storyId") Long storyId, @Param("sumDelta") int sumDelta,
                  @Param("countDelta") int countDelta);

    @Modifying
    @Query("UPDATE Story s SET s.commentCount = s.commentCount + :delta WHERE s.id = :storyId")
    int addCommentCount(@Param("storyId") Long storyId, @Param("delta") int delta);

    /** Slug của truyện đã xóa mềm vẫn được tính là đã dùng. */
    boolean existsBySlug(String slug);

    /** Truyện chưa xóa của một tác giả, cho danh sách trong khu vực tác giả. */
    Page<Story> findByAuthorIdAndDeletedAtIsNull(Long authorId, Pageable pageable);

    List<Story> findByAuthorIdAndDeletedAtIsNull(Long authorId, Sort sort);

    /**
     * Ghi nhận một chương vừa được đăng: cộng số chương và đẩy truyện lên đầu danh sách "mới cập nhật".
     * Chỉ ChapterPublishService được gọi.
     */
    @Modifying
    @Query("""
            UPDATE Story s SET s.chapterCount = s.chapterCount + 1, s.lastChapterAt = :publishedAt
            WHERE s.id = :storyId
            """)
    int recordChapterPublished(@Param("storyId") Long storyId, @Param("publishedAt") Instant publishedAt);

    /** Tổng số liệu trên mọi truyện chưa xóa của một tác giả; các tổng là NULL khi tác giả chưa có truyện. */
    @Query("""
            SELECT new vn.edu.utc.comic.stats.dto.AuthorStatsOverview(
                COUNT(s), SUM(s.chapterCount), SUM(s.viewCount), SUM(s.followCount), SUM(s.commentCount),
                SUM(s.ratingSum), SUM(s.ratingCount))
            FROM Story s WHERE s.author.id = :authorId AND s.deletedAt IS NULL
            """)
    AuthorStatsOverview summarizeByAuthor(@Param("authorId") Long authorId);

    /** Số truyện đang công khai theo loại, cho trang tổng quan quản trị. */
    @Query("""
            SELECT COUNT(s) FROM Story s
            WHERE s.type = :type AND s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED
              AND s.deletedAt IS NULL
            """)
    long countPublicByType(@Param("type") StoryType type);

    /** Ẩn / gỡ ẩn một chương đã đăng làm số chương hiển thị đổi theo. Chỉ ChapterPublishService được gọi. */
    @Modifying
    @Query("UPDATE Story s SET s.chapterCount = s.chapterCount + :delta WHERE s.id = :storyId")
    int addChapterCount(@Param("storyId") Long storyId, @Param("delta") int delta);

    /**
     * Id các truyện công khai chung nhiều thể loại nhất với một truyện (cho hàm "truyện tương tự" của chatbot):
     * nhiều thể loại chung trước, cùng số thì truyện nhiều lượt xem trước.
     */
    @Query("""
            SELECT s.id FROM Story s JOIN s.genres g
            WHERE g.id IN (SELECT g2.id FROM Story s2 JOIN s2.genres g2 WHERE s2.id = :storyId)
              AND s.id <> :storyId
              AND s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED AND s.deletedAt IS NULL
            GROUP BY s.id
            ORDER BY COUNT(g.id) DESC, MAX(s.viewCount) DESC, s.id DESC
            """)
    List<Long> findSimilarStoryIds(@Param("storyId") Long storyId, Pageable pageable);
}
