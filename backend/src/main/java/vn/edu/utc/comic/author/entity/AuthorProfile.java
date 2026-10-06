package vn.edu.utc.comic.author.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.user.entity.UserAccount;

/**
 * Hồ sơ tác giả, quan hệ 1:1 với tài khoản và dùng chung khóa chính.
 * Chỉ được tạo khi yêu cầu đăng ký tác giả được duyệt.
 */
@Getter
@Setter
@Entity
@Table(name = "author_profile")
public class AuthorProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    /** Bút danh hiển thị trên trang truyện, duy nhất toàn hệ thống. */
    @Column(name = "pen_name", nullable = false, unique = true, length = 100)
    private String penName;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;
}
