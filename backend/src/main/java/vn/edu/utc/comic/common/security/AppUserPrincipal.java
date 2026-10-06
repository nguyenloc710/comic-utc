package vn.edu.utc.comic.common.security;

import java.io.Serializable;
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
    private final String email;
    private final boolean locked;
    private final AccountState accountState;
    private final String initials;
    private final transient Collection<? extends GrantedAuthority> authorities;

    private AppUserPrincipal(Identity identity, AccountState accountState) {
        this.id = identity.id();
        this.username = identity.username();
        this.password = identity.passwordHash();
        this.email = identity.email();
        this.locked = identity.locked();
        this.accountState = accountState;
        this.initials = buildInitials(accountState.displayName());
        this.authorities = List.of(
                new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + accountState.role().name()));
    }

    /**
     * @param user tài khoản vừa nạp từ cơ sở dữ liệu
     * @param now  thời điểm hiện tại, để xác định tài khoản có đang bị khóa tạm hay không
     */
    public static AppUserPrincipal from(UserAccount user, Instant now) {
        Identity identity = new Identity(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getEmail(), user.isLockedAt(now));
        AccountState state = new AccountState(user.getRole(), user.getStatus(), user.getDisplayName(),
                user.getAvatarPath());
        return new AppUserPrincipal(identity, state);
    }

    /** Bản sao mang trạng thái mới (vai trò, tên hiển thị...), dùng khi làm mới phiên đang đăng nhập. */
    public AppUserPrincipal withState(AccountState newState) {
        return new AppUserPrincipal(new Identity(id, username, password, email, locked), newState);
    }

    public Role getRole() {
        return accountState.role();
    }

    public String getDisplayName() {
        return accountState.displayName();
    }

    /** Khóa ảnh đại diện; URL dựng qua StorageService.resolveUrl. */
    public String getAvatarPath() {
        return accountState.avatarPath();
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
        return accountState.isActive();
    }

    /** Phần không đổi trong suốt phiên đăng nhập. */
    private record Identity(Long id, String username, String passwordHash, String email, boolean locked)
            implements Serializable {
    }
}
