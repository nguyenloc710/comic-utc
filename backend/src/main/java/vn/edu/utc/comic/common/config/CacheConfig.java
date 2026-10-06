package vn.edu.utc.comic.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.utc.comic.common.constant.CacheConstants;

/**
 * Cache trong bộ nhớ cho dữ liệu ít thay đổi nhưng đọc ở hầu hết request.
 * Truyện và chương KHÔNG cache ở đây vì bộ đếm thay đổi liên tục.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final long DEFAULT_MAX_SIZE = 500;

    /**
     * Trạng thái tài khoản được xóa khỏi cache ngay khi có thay đổi qua ứng dụng; thời hạn ngắn chỉ là lưới
     * an toàn cho thay đổi không đi qua ứng dụng (sửa thẳng cơ sở dữ liệu).
     */
    private static final Duration ACCOUNT_STATE_TTL = Duration.ofMinutes(5);
    private static final long ACCOUNT_STATE_MAX_SIZE = 10_000;

    /** Bảng xếp hạng tốn một truy vấn gom nhóm và không cần chính xác từng phút. */
    private static final Duration RANKING_TTL = Duration.ofMinutes(10);
    private static final long RANKING_MAX_SIZE = 20;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CacheConstants.SETTINGS, CacheConstants.GENRES);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_TTL)
                .maximumSize(DEFAULT_MAX_SIZE));
        cacheManager.registerCustomCache(CacheConstants.ACCOUNT_STATES, Caffeine.newBuilder()
                .expireAfterWrite(ACCOUNT_STATE_TTL)
                .maximumSize(ACCOUNT_STATE_MAX_SIZE)
                .build());
        cacheManager.registerCustomCache(CacheConstants.RANKINGS, Caffeine.newBuilder()
                .expireAfterWrite(RANKING_TTL)
                .maximumSize(RANKING_MAX_SIZE)
                .build());
        return cacheManager;
    }
}
