package vn.edu.utc.comic.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Lớp cha cho test tích hợp: MySQL 8.4 thật do Testcontainers cấp, Flyway chạy đúng migration của ứng dụng.
 *
 * <p>Container được khởi động MỘT lần trong khối static và dùng chung cho mọi lớp test (không dùng
 * {@code @Container}): Spring cache lại ApplicationContext giữa các lớp test, nên nếu mỗi lớp có container
 * riêng thì context đã cache sẽ trỏ vào một cơ sở dữ liệu đã bị tắt. Ryuk dọn container khi JVM kết thúc.
 * Image lấy qua mirror ECR Public vì Docker Hub có thể bị chặn trên mạng nội bộ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("public.ecr.aws/docker/library/mysql:8.4").asCompatibleSubstituteFor("mysql"))
            // Cùng tham số với docker-compose.yml để hành vi FULLTEXT và múi giờ giống môi trường dev
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci",
                    "--innodb-ft-min-token-size=1", "--default-time-zone=+00:00");

    static {
        MYSQL.start();
    }
}
