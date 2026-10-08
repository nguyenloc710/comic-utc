package vn.edu.utc.comic.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.common.security.AccountState;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

/** Truy vấn tài khoản. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {

    /** Tìm theo tên đăng nhập hoặc email — người dùng được nhập một trong hai ở ô đăng nhập. */
    @Query("SELECT u FROM UserAccount u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<UserAccount> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    @Query("SELECT u.id FROM UserAccount u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<Long> findIdByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /** Chỉ lấy bốn cột mà phiên đăng nhập cần so sánh; truy vấn này chạy mỗi khi cache trạng thái hết hạn. */
    @Query("""
            SELECT new vn.edu.utc.comic.common.security.AccountState(u.role, u.status, u.displayName, u.avatarPath)
            FROM UserAccount u WHERE u.id = :userId
            """)
    Optional<AccountState> findAccountStateById(@Param("userId") Long userId);

    /** So sánh không phân biệt hoa thường nhờ collation utf8mb4_unicode_ci của cột. */
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE UserAccount u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.id = :userId")
    int incrementFailedAttempts(@Param("userId") Long userId);

    /** Khóa tạm và đưa bộ đếm về 0 nếu số lần sai đã chạm ngưỡng; trả về 1 khi vừa khóa. */
    @Modifying
    @Query("""
            UPDATE UserAccount u SET u.lockedUntil = :lockedUntil, u.failedAttempts = 0
            WHERE u.id = :userId AND u.failedAttempts >= :maxAttempts
            """)
    int lockIfAttemptsReached(@Param("userId") Long userId, @Param("maxAttempts") int maxAttempts,
                              @Param("lockedUntil") Instant lockedUntil);

    @Modifying
    @Query("""
            UPDATE UserAccount u SET u.failedAttempts = 0, u.lockedUntil = NULL, u.lastLoginAt = :loginAt
            WHERE u.id = :userId
            """)
    int resetFailedAttempts(@Param("userId") Long userId, @Param("loginAt") Instant loginAt);

    long countByRole(Role role);

    /** Số quản trị viên còn hoạt động: hệ thống không bao giờ được về 0. */
    long countByRoleAndStatus(Role role, UserStatus status);

    /** Thời điểm đăng ký của các tài khoản tạo từ một mốc, để gom số đăng ký theo ngày trên biểu đồ. */
    @Query("SELECT u.createdAt FROM UserAccount u WHERE u.createdAt >= :from")
    List<Instant> findRegistrationTimesSince(@Param("from") Instant from);

    /**
     * Khóa dòng tài khoản tới hết transaction, để các thao tác đếm theo người dùng (hạn mức chat mỗi ngày) chạy
     * lần lượt. Phải là câu lệnh đầu tiên của transaction (xem ghi chú ở StoryRepository.lockForCounterUpdate).
     */
    @Query(value = "SELECT id FROM user_account WHERE id = :userId FOR UPDATE", nativeQuery = true)
    Optional<Long> lockForUpdate(@Param("userId") Long userId);
}
