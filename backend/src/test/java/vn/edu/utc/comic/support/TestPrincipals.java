package vn.edu.utc.comic.support;

import java.time.Instant;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/** Dựng principal cho test mà không cần tài khoản thật trong cơ sở dữ liệu. */
public final class TestPrincipals {

    private static final long TEST_USER_ID = 9_000L;

    public static AppUserPrincipal withRole(Role role) {
        UserAccount user = new UserAccount();
        user.setId(TEST_USER_ID + role.ordinal());
        user.setUsername("test-" + role.name().toLowerCase());
        user.setEmail(role.name().toLowerCase() + "@test.local");
        user.setPasswordHash("unused");
        user.setDisplayName("Người Thử " + role.name());
        user.setRole(role);
        return AppUserPrincipal.from(user, Instant.now());
    }

    private TestPrincipals() {
        throw new UnsupportedOperationException("Utility class");
    }
}
