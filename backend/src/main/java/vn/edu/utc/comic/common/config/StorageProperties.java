package vn.edu.utc.comic.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình nơi lưu ảnh (bìa, trang truyện, avatar).
 *
 * @param root              thư mục gốc lưu ảnh người dùng tải lên; cơ sở dữ liệu chỉ giữ khóa tương đối so với nó
 * @param demoMediaLocation nơi chứa ảnh của dữ liệu demo (chỉ đặt ở profile dev), ví dụ
 *                          {@code classpath:/demo-media/}; để trống ở các môi trường khác
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(@NotBlank String root, String demoMediaLocation) {
}
