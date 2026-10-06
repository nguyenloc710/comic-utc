package vn.edu.utc.comic.common.security;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

/**
 * Thông tin tài khoản mà Spring Security giữ trong phiên đăng nhập.
 * Chỉ mang dữ liệu tối thiểu, KHÔNG giữ tham chiếu tới thực thể JPA.
 */
@Getter
public class AppUserPrincipal implements UserDetails {

    private static final int INITIALS_LENGTH = 2;

    private final Long id;
    private final String username;
    private final transient String password;
    private final String displayName;
    private final String email;
    private final Role role;
    private final String initials;
    private final boolean active;
    private final boolean locked;
    private final transient Collection<? extends GrantedAuthority> authorities;

    private AppUserPrincipal(UserAccount user, Instant now) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.initials = buildInitials(user.getDisplayName());
        this.active = user.getStatus() == UserStatus.ACTIVE;
        this.locked = user.isLockedAt(now);
        this.authorities = List.of(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role.name()));
    }

    /**
     * @param user tài khoản vừa nạp từ cơ sở dữ liệu
     * @param now  thời điểm hiện tại, để xác định tài khoản có đang bị khóa tạm hay không
     */
    public static AppUserPrincipal from(UserAccount user, Instant now) {
        return new AppUserPrincipal(user, now);
    }

    /** Chữ cái đầu của từ đầu và từ cuối trong tên hiển thị, để vẽ avatar khi chưa có ảnh. */
    private static String buildInitials(String displayName) {
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(INITIALS_LENGTH, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
