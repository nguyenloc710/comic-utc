package vn.edu.utc.comic.chapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.entity.AuditableEntity;
import vn.edu.utc.comic.story.entity.Story;

/**
 * Một chương của truyện. Nội dung nằm ở bảng riêng: {@link ChapterPage} với truyện tranh,
 * {@link ChapterContent} với truyện chữ — để danh sách chương không kéo theo dữ liệu nặng.
 */
@Getter
@Setter
@Entity
@Table(name = "chapter")
public class Chapter extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    /** Số thứ tự chương, duy nhất trong truyện, không đổi sau khi đã đăng. */
    @Column(name = "chapter_no", nullable = false)
    private int chapterNo;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChapterStatus status = ChapterStatus.DRAFT;

    /** Giờ hẹn đăng (UTC); chỉ có nghĩa khi status = SCHEDULED. */
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "hidden_reason", length = 1000)
    private String hiddenReason;

    /**
     * Số ảnh của chương truyện tranh. Chỉ đổi qua câu UPDATE ở repository mỗi lần thêm / xóa ảnh: ảnh được tải
     * lên bằng nhiều request trong lúc form soạn chương vẫn mở, nên nếu để Hibernate ghi cột này khi lưu form
     * thì một lần bấm "Lưu" sẽ ghi đè số ảnh bằng giá trị cũ.
     */
    @Column(name = "page_count", nullable = false, updatable = false)
    private int pageCount;

    /** Số từ của chương truyện chữ. */
    @Column(name = "word_count", nullable = false)
    private int wordCount;

    /** Chỉ đổi qua câu UPDATE cộng dồn ở repository (cùng lý do với bộ đếm của Story). */
    @Column(name = "view_count", nullable = false, updatable = false)
    private long viewCount;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
