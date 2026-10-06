package vn.edu.utc.comic.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.common.config.StorageProperties;
import vn.edu.utc.comic.common.constant.StorageConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;

/**
 * Lưu ảnh vào đĩa máy chủ, phục vụ lại qua /media/** (xem WebMvcConfig).
 *
 * <p>Tên tệp do máy chủ sinh bằng UUID, KHÔNG dùng tên người dùng gửi lên — tránh ghi đè, tránh tấn công
 * vượt thư mục, và làm cho URL của ảnh thuộc chương nháp không đoán được.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private static final String KEY_SEPARATOR = "/";

    private final StorageProperties storageProperties;
    private final ImageValidator imageValidator;

    @Override
    public StoredFile storeImage(MultipartFile file, String directory) {
        ImageInfo imageInfo = imageValidator.validate(file);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + imageInfo.extension();
        String key = directory + KEY_SEPARATOR + storedName;
        Path target = resolveInsideRoot(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
        } catch (IOException exception) {
            log.error("Không lưu được ảnh vào {}", key, exception);
            throw new ApiException(ErrorCode.STORAGE_FAILED, exception);
        }
        return new StoredFile(key, imageInfo.width(), imageInfo.height(), file.getSize());
    }

    @Override
    public void delete(String key) {
        Path target = resolveInsideRoot(key);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            // Tệp mồ côi chỉ tốn dung lượng đĩa; không đáng để làm hỏng thao tác nghiệp vụ đang xóa bản ghi
            log.warn("Không xóa được ảnh {}", key, exception);
        }
    }

    @Override
    public String resolveUrl(String key) {
        return key == null || key.isBlank() ? null : StorageConstants.MEDIA_URL_PREFIX + key;
    }

    /** Chặn mọi khóa thoát ra ngoài thư mục gốc ("..", đường dẫn tuyệt đối). */
    private Path resolveInsideRoot(String key) {
        Path root = Path.of(storageProperties.root()).toAbsolutePath().normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            log.warn("Từ chối khóa lưu trữ nằm ngoài thư mục gốc: {}", key);
            throw new ApiException(ErrorCode.STORAGE_FAILED);
        }
        return target;
    }
}
