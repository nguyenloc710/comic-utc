package vn.edu.utc.comic.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.user.entity.UserAccount;

/** Truy vấn tài khoản. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** Tìm theo tên đăng nhập hoặc email — người dùng được nhập một trong hai ở ô đăng nhập. */
    @Query("SELECT u FROM UserAccount u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<UserAccount> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
}
