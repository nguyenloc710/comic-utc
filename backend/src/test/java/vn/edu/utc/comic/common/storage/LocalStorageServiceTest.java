package vn.edu.utc.comic.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.comic.common.config.StorageProperties;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.setting.SettingService;

class LocalStorageServiceTest {

    private static final String COVER_DIRECTORY = "stories/12/cover";
    private static final int MAX_SIZE_MB = 5;
    private static final int WIDTH = 64;
    private static final int HEIGHT = 96;

    @TempDir
    private Path storageRoot;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        SettingService settingService = mock(SettingService.class);
        when(settingService.getInt(anyString(), anyInt())).thenReturn(MAX_SIZE_MB);
        storageService = new LocalStorageService(
                new StorageProperties(storageRoot.toString()), new ImageValidator(settingService));
    }

    @Test
    void storeImage_writesFileUnderDirectoryWithGeneratedName() throws IOException {
        StoredFile stored = storageService.storeImage(pngFile("../../ten-nguoi-dung-gui.png"), COVER_DIRECTORY);

        assertThat(stored.key()).startsWith(COVER_DIRECTORY + "/").endsWith(".png")
                .doesNotContain("ten-nguoi-dung-gui");
        assertThat(stored.width()).isEqualTo(WIDTH);
        assertThat(stored.height()).isEqualTo(HEIGHT);
        assertThat(storageRoot.resolve(stored.key())).isRegularFile().hasSize(stored.sizeBytes());
        assertThat(storageService.resolveUrl(stored.key())).isEqualTo("/media/" + stored.key());
    }

    @Test
    void storeImage_writesNothing_whenFileIsNotAnImage() throws IOException {
        MockMultipartFile fake = new MockMultipartFile("file", "anh.png", "image/png",
                "khong phai anh".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storageService.storeImage(fake, COVER_DIRECTORY))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IMAGE_TYPE_NOT_ALLOWED));
        try (Stream<Path> files = Files.walk(storageRoot)) {
            assertThat(files.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside", "stories/../../outside"})
    void storeImage_rejectsDirectoryEscapingRoot(String directory) throws IOException {
        MockMultipartFile file = pngFile("a.png");

        assertThatThrownBy(() -> storageService.storeImage(file, directory))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORAGE_FAILED));
    }

    @Test
    void delete_removesStoredFile_andIgnoresMissingOne() throws IOException {
        StoredFile stored = storageService.storeImage(pngFile("a.png"), COVER_DIRECTORY);

        storageService.delete(stored.key());
        storageService.delete(stored.key());

        assertThat(storageRoot.resolve(stored.key())).doesNotExist();
    }

    @Test
    void delete_rejectsKeyEscapingRoot() throws IOException {
        Path outside = Files.createTempFile("comic-outside", ".txt");
        try {
            String escapingKey = storageRoot.relativize(outside).toString();

            assertThatThrownBy(() -> storageService.delete(escapingKey)).isInstanceOf(ApiException.class);
            assertThat(outside).exists();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void resolveUrl_returnsNull_whenNoImage() {
        assertThat(storageService.resolveUrl(null)).isNull();
        assertThat(storageService.resolveUrl(" ")).isNull();
    }

    private static MockMultipartFile pngFile(String originalName) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("file", originalName, "image/png", output.toByteArray());
    }
}
