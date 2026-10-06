package vn.edu.utc.comic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Điểm khởi động website đọc và đăng tải truyện tranh, truyện chữ. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ComicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComicApplication.class, args);
    }
}
