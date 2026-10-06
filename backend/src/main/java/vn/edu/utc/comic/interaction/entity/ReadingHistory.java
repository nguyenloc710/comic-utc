package vn.edu.utc.comic.interaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Chương đang đọc dở của một người ở một truyện — nguồn cho nút "Đọc tiếp" và trang lịch sử. */
@Getter
@Setter
@Entity
@Table(name = "reading_history")
public class ReadingHistory {

    @EmbeddedId
    private UserStoryId id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
