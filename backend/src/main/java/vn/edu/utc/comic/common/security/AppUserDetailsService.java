package vn.edu.utc.comic.common.security;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Nạp tài khoản cho Spring Security. Cho phép đăng nhập bằng tên đăng nhập hoặc email.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        return userAccountRepository.findByUsernameOrEmail(usernameOrEmail)
                .map(user -> AppUserPrincipal.from(user, clock.instant()))
                .orElseThrow(() -> new UsernameNotFoundException(usernameOrEmail));
    }
}
