package vn.edu.utc.comic.common.config;

import java.nio.file.Path;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.edu.utc.comic.common.constant.StorageConstants;

/**
 * Cấu hình MVC: phục vụ ảnh đã tải lên dưới /media/**.
 *
 * <p>Ảnh bìa và ảnh chương là nội dung công khai (khách vãng lai cũng đọc được) nên được phục vụ tĩnh
 * thay vì qua controller kiểm tra quyền. Bản nháp không lộ nhờ tên tệp là UUID không đoán được.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Duration MEDIA_CACHE_DURATION = Duration.ofDays(365);

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Tên tệp là UUID và không bao giờ bị ghi đè, nên trình duyệt được phép cache dài hạn
        registry.addResourceHandler(StorageConstants.MEDIA_URL_PREFIX + "**")
                .addResourceLocations(resolveMediaLocation())
                .setCacheControl(CacheControl.maxAge(MEDIA_CACHE_DURATION).cachePublic());
    }

    /** Location của Spring phải kết thúc bằng "/"; toUri() chỉ tự thêm khi thư mục đã tồn tại. */
    private String resolveMediaLocation() {
        String location = Path.of(storageProperties.root()).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
