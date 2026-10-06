package vn.edu.utc.comic.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Thư mục gốc lưu ảnh (bìa, trang truyện, avatar). Cơ sở dữ liệu chỉ giữ khóa tương đối so với thư mục này.
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(@NotBlank String root) {
}
