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
 * Cache trong bộ nhớ cho dữ liệu ít thay đổi nhưng đọc ở hầu hết request (tham số vận hành).
 * Truyện và chương KHÔNG cache ở đây vì bộ đếm thay đổi liên tục.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final long MAX_CACHE_SIZE = 500;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CacheConstants.SETTINGS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL)
                .maximumSize(MAX_CACHE_SIZE));
        return cacheManager;
    }
}
