package vn.edu.utc.comic.author.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;
import vn.edu.utc.comic.author.enums.IntendedStoryType;
import vn.edu.utc.comic.common.entity.AuditableEntity;
import vn.edu.utc.comic.user.entity.UserAccount;

/**
 * Yêu cầu đăng ký làm tác giả của một độc giả.
 *
 * <p>Bảng còn cột sinh {@code pending_user_id} (không map ở đây) mang UNIQUE để cơ sở dữ liệu tự chặn
 * hai yêu cầu PENDING của cùng một người khi hai request tới cùng lúc.
 */
@Getter
@Setter
@Entity
@Table(name = "author_request")
public class AuthorRequest extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "pen_name", nullable = false, length = 100)
    private String penName;

    @Column(name = "introduction", nullable = false, length = 2000)
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "intended_type", nullable = false, length = 20)
    private IntendedStoryType intendedType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuthorRequestStatus status = AuthorRequestStatus.PENDING;

    /** Bắt buộc khi từ chối; hiển thị lại cho người gửi. */
    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
