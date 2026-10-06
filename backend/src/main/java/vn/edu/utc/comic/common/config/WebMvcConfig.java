package vn.edu.utc.comic.common.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
                .addResourceLocations(resolveMediaLocations().toArray(String[]::new))
                .setCacheControl(CacheControl.maxAge(MEDIA_CACHE_DURATION).cachePublic());
    }

    /** Ảnh người dùng tải lên được tìm trước; ảnh của dữ liệu demo (nếu có cấu hình) là nơi tìm thứ hai. */
    private List<String> resolveMediaLocations() {
        List<String> locations = new ArrayList<>();
        locations.add(resolveUploadLocation());
        String demoLocation = storageProperties.demoMediaLocation();
        if (demoLocation != null && !demoLocation.isBlank()) {
            locations.add(demoLocation);
        }
        return locations;
    }

    /** Location của Spring phải kết thúc bằng "/"; toUri() chỉ tự thêm khi thư mục đã tồn tại. */
    private String resolveUploadLocation() {
        String location = Path.of(storageProperties.root()).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
