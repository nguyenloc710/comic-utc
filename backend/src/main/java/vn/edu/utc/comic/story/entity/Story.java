package vn.edu.utc.comic.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.AuditableEntity;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.user.entity.UserAccount;

/**
 * Truyện (tranh hoặc chữ).
 *
 * <p>Các cột bộ đếm ({@code *_count}, {@code rating_sum}) khai báo {@code updatable = false}: Hibernate ghi
 * cả dòng khi lưu thực thể, nên nếu để cập nhật được thì một lần sửa tên truyện sẽ ghi đè lượt xem vừa tăng
 * bằng giá trị cũ đã đọc lên trước đó. Bộ đếm chỉ đổi qua câu UPDATE cộng dồn ở repository.
 */
@Getter
@Setter
@Entity
@Table(name = "story")
public class Story extends AuditableEntity {

    /** Sinh từ tên lúc tạo, không đổi khi sửa tên để link cũ không chết. */
    @Column(name = "slug", nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "alt_title", length = 200)
    private String altTitle;

    /** Text thuần, không phải HTML. */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_path", length = 500)
    private String coverPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private StoryType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoryStatus status = StoryStatus.ONGOING;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private StoryVisibility visibility = StoryVisibility.DRAFT;

    /** Lý do quản trị viên ẩn truyện; tác giả đọc được trong studio. */
    @Column(name = "hidden_reason", length = 1000)
    private String hiddenReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    /** Sắp theo thứ tự hiển thị của thể loại để nhãn trên thẻ truyện luôn ổn định giữa các lần tải. */
    @ManyToMany
    @OrderBy("sortOrder ASC")
    @JoinTable(name = "story_genre",
            joinColumns = @JoinColumn(name = "story_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new LinkedHashSet<>();

    /** Chỉ đếm chương đã đăng (PUBLISHED). */
    @Column(name = "chapter_count", nullable = false, updatable = false)
    private int chapterCount;

    @Column(name = "view_count", nullable = false, updatable = false)
    private long viewCount;

    @Column(name = "follow_count", nullable = false, updatable = false)
    private int followCount;

    /** Điểm trung bình = rating_sum / rating_count, tính lúc ánh xạ để không lệch do làm tròn. */
    @Column(name = "rating_sum", nullable = false, updatable = false)
    private int ratingSum;

    @Column(name = "rating_count", nullable = false, updatable = false)
    private int ratingCount;

    @Column(name = "comment_count", nullable = false, updatable = false)
    private int commentCount;

    /** Lần đầu truyện được công khai. */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Thời điểm đăng chương gần nhất, dùng để sắp xếp "mới cập nhật". */
    @Column(name = "last_chapter_at")
    private Instant lastChapterAt;

    /** Khác null = đã xóa mềm. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Chống hai lần lưu form sửa truyện ghi đè nhau; bộ đếm không làm tăng version. */
    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
