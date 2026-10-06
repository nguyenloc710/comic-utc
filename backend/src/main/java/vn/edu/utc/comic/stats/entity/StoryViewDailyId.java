package vn.edu.utc.comic.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;

/** Khóa chính ghép (truyện, ngày theo giờ Việt Nam) của bảng lượt xem theo ngày. */
@Embeddable
public record StoryViewDailyId(
        @Column(name = "story_id") Long storyId,
        @Column(name = "view_date") LocalDate viewDate) implements Serializable {
}
