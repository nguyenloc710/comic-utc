package vn.edu.utc.comic.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nguồn thời gian duy nhất của ứng dụng.
 *
 * <p>Service và job nhận Clock qua constructor thay vì gọi Instant.now(), nhờ vậy test cố định được
 * thời gian để kiểm chứng hẹn giờ đăng chương, khóa tạm tài khoản, thời gian chờ gửi lại yêu cầu.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
