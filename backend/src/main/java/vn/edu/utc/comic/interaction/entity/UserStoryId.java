package vn.edu.utc.comic.interaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

/**
 * Khóa chính ghép (người dùng, truyện) dùng chung cho theo dõi, lịch sử đọc và đánh giá:
 * mỗi người có tối đa một dòng cho mỗi truyện ở từng bảng.
 */
@Embeddable
public record UserStoryId(
        @Column(name = "user_id") Long userId,
        @Column(name = "story_id") Long storyId) implements Serializable {
}
