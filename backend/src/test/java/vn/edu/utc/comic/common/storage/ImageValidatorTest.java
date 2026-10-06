package vn.edu.utc.comic.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;

@ExtendWith(MockitoExtension.class)
class ImageValidatorTest {

    private static final int MAX_SIZE_MB = 1;
    private static final int BYTES_PER_MB = 1024 * 1024;
    private static final int WIDTH = 40;
    private static final int HEIGHT = 30;
    /** Ảnh WebP 1x1 hợp lệ nhỏ nhất, để kiểm chứng plugin đọc WebP đã được nạp. */
    private static final String ONE_PIXEL_WEBP_BASE64 = "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA";

    @Mock
    private SettingService settingService;

    @InjectMocks
    private ImageValidator imageValidator;

    @BeforeEach
    void setUp() {
        // lenient: tệp rỗng bị từ chối trước khi cần đọc giới hạn dung lượng
        lenient().when(settingService.getInt(eq(SettingKeys.UPLOAD_IMAGE_MAX_SIZE_MB), anyInt())).thenReturn(MAX_SIZE_MB);
    }

    @Test
    void validate_readsFormatFromContent_notFromFileName() throws IOException {
        MockMultipartFile pngNamedAsJpg = new MockMultipartFile("file", "trang-1.jpg", "image/jpeg", encode("png"));

        ImageInfo info = imageValidator.validate(pngNamedAsJpg);

        assertThat(info).isEqualTo(new ImageInfo("png", WIDTH, HEIGHT));
    }

    @Test
    void validate_acceptsJpeg() throws IOException {
        MockMultipartFile jpeg = new MockMultipartFile("file", "bia.jpeg", "image/jpeg", encode("jpg"));

        assertThat(imageValidator.validate(jpeg)).isEqualTo(new ImageInfo("jpg", WIDTH, HEIGHT));
    }

    @Test
    void validate_acceptsWebp() {
        byte[] webp = Base64.getDecoder().decode(ONE_PIXEL_WEBP_BASE64);
        MockMultipartFile file = new MockMultipartFile("file", "trang.webp", "image/webp", webp);

        assertThat(imageValidator.validate(file)).isEqualTo(new ImageInfo("webp", 1, 1));
    }

    @Test
    void validate_rejectsNonImageDisguisedByExtension() {
        byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fake = new MockMultipartFile("file", "anh.png", "image/png", script);

        assertErrorCode(fake, ErrorCode.IMAGE_TYPE_NOT_ALLOWED);
    }

    @Test
    void validate_rejectsRealImageOfUnsupportedFormat() throws IOException {
        MockMultipartFile gif = new MockMultipartFile("file", "anh.gif", "image/gif", encode("gif"));

        assertErrorCode(gif, ErrorCode.IMAGE_TYPE_NOT_ALLOWED);
    }

    @Test
    void validate_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "rong.png", "image/png", new byte[0]);

        assertErrorCode(empty, ErrorCode.IMAGE_TYPE_NOT_ALLOWED);
    }

    @Test
    void validate_rejectsFileOverConfiguredLimit() {
        byte[] oversized = new byte[MAX_SIZE_MB * BYTES_PER_MB + 1];
        MockMultipartFile file = new MockMultipartFile("file", "to.png", "image/png", oversized);

        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
                    assertThat(exception.getArguments()).containsExactly(MAX_SIZE_MB);
                });
    }

    private void assertErrorCode(MockMultipartFile file, ErrorCode expected) {
        assertThatThrownBy(() -> imageValidator.validate(file))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private static byte[] encode(String format) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
