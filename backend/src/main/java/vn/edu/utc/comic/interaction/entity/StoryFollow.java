package vn.edu.utc.comic.interaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Một người đang theo dõi một truyện; người theo dõi nhận thông báo khi truyện ra chương mới. */
@Getter
@Setter
@Entity
@Table(name = "story_follow")
@EntityListeners(AuditingEntityListener.class)
public class StoryFollow {

    @EmbeddedId
    private UserStoryId id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
