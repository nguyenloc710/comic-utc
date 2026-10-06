package vn.edu.utc.comic.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.AuditableEntity;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

/** Tài khoản đăng nhập của mọi vai trò. Mỗi tài khoản có đúng một vai trò. */
@Getter
@Setter
@Entity
@Table(name = "user_account")
public class UserAccount extends AuditableEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** Khóa ảnh tương đối; URL dựng qua StorageService.resolveUrl. */
    @Column(name = "avatar_path", length = 500)
    private String avatarPath;

    @Column(name = "bio", length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Khóa tạm do sai mật khẩu nhiều lần; khác với status BANNED do quản trị viên đặt. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Tài khoản có đang bị khóa tạm tại thời điểm cho trước hay không. */
    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
