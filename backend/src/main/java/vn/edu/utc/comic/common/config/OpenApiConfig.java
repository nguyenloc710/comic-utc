package vn.edu.utc.comic.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tài liệu OpenAPI cho nhóm /api/** (Swagger UI tại /swagger-ui.html, chỉ ADMIN xem được).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI comicOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Comic UTC API")
                .version("v1")
                .description("""
                        API JSON dùng bởi giao diện Thymeleaf: chatbot, theo dõi, đánh giá, bình luận, thông báo,
                        tải ảnh chương, dữ liệu biểu đồ.
                        Xác thực bằng phiên đăng nhập (cookie) và CSRF token của trang.
                        """));
    }
}
