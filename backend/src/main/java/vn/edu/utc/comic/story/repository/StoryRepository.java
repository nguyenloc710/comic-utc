package vn.edu.utc.comic.story.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.story.dto.StoryRatingSummary;
import vn.edu.utc.comic.story.entity.Story;

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
}
