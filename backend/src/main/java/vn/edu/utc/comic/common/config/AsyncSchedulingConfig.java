package vn.edu.utc.comic.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bật job định kỳ (đăng chương hẹn giờ) và xử lý bất đồng bộ (thông báo, đếm lượt xem sau commit).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncSchedulingConfig {

    public static final String EVENT_EXECUTOR = "eventExecutor";

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 500;

    /** Pool riêng cho việc chạy nền để không chiếm luồng phục vụ request đọc truyện. */
    @Bean(name = EVENT_EXECUTOR)
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}
