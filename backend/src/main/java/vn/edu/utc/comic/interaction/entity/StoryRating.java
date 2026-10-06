package vn.edu.utc.comic.interaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Đánh giá sao của một người cho một truyện; mỗi người một lần, sửa được. */
@Getter
@Setter
@Entity
@Table(name = "story_rating")
public class StoryRating {

    @EmbeddedId
    private UserStoryId id;

    /** Từ 1 đến 5 (CHECK ở cơ sở dữ liệu). */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "stars", nullable = false)
    private int stars;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
