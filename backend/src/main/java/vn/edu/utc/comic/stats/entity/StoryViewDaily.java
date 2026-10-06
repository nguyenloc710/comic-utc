package vn.edu.utc.comic.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Lượt xem của một truyện trong một ngày — nguồn cho bảng xếp hạng ngày/tuần/tháng và biểu đồ của tác giả.
 * Ghi bằng INSERT ... ON DUPLICATE KEY UPDATE ở repository, thực thể này chỉ để đọc.
 */
@Getter
@Setter
@Entity
@Table(name = "story_view_daily")
public class StoryViewDaily {

    @EmbeddedId
    private StoryViewDailyId id;

    @Column(name = "view_count", nullable = false)
    private int viewCount;
}
