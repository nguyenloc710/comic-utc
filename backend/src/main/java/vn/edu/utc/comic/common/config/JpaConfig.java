package vn.edu.utc.comic.common.config;

import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import vn.edu.utc.comic.common.security.SecurityUtils;

/**
 * Bật tự ghi thông tin kiểm toán (người tạo, người sửa, thời điểm) cho thực thể kế thừa
 * CreatedAtEntity / AuditableEntity.
 */
@Configuration
@RequiredArgsConstructor
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    private final Clock clock;

    /** Id tài khoản đang thao tác; job chạy nền và người tự đăng ký không có người dùng nên giá trị rỗng là hợp lệ. */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.ofNullable(SecurityUtils.getCurrentUserId());
    }

    /** Dùng cùng Clock với nghiệp vụ để test cố định được thời gian của cột kiểm toán. */
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(clock.instant());
    }
}
