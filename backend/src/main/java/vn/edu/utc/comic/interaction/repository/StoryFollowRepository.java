package vn.edu.utc.comic.interaction.repository;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.interaction.entity.StoryFollow;
import vn.edu.utc.comic.interaction.entity.UserStoryId;

/** Truy vấn quan hệ theo dõi truyện. */
public interface StoryFollowRepository extends JpaRepository<StoryFollow, UserStoryId> {

    /**
     * Thêm quan hệ theo dõi nếu chưa có. Trả về 1 khi vừa thêm, 0 khi đã theo dõi từ trước — service dựa vào
     * con số này để chỉ cộng bộ đếm khi thật sự có dòng mới, kể cả lúc hai request tới cùng lúc.
     * Phải là SQL gốc vì JPQL không có INSERT IGNORE.
     */
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO story_follow (user_id, story_id, created_at) VALUES (:userId, :storyId, :createdAt)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("storyId") Long storyId,
                       @Param("createdAt") Instant createdAt);

    /** Trả về 1 khi vừa bỏ theo dõi, 0 khi vốn không theo dõi. */
    @Modifying
    @Query("DELETE FROM StoryFollow f WHERE f.id.userId = :userId AND f.id.storyId = :storyId")
    int deleteFollow(@Param("userId") Long userId, @Param("storyId") Long storyId);
}
