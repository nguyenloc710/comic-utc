package vn.edu.utc.comic.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.common.constant.StorageConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;

/**
 * Kiểm tra ảnh người dùng tải lên trước khi lưu.
 *
 * <p>Định dạng được xác định từ NỘI DUNG tệp (ImageIO đọc phần đầu ảnh), không dựa vào đuôi hay MIME
 * do trình duyệt gửi — hai thứ đó kẻ tấn công đặt tùy ý. Tệp không phải JPEG/PNG/WebP thật thì bị từ chối,
 * kể cả khi mang đuôi .jpg.
 */
@Component
@RequiredArgsConstructor
public class ImageValidator {

    private final SettingService settingService;

    /**
     * @return định dạng thật và kích thước của ảnh
     * @throws ApiException IMAGE_TOO_LARGE nếu vượt dung lượng cho phép (đọc từ setting),
     *                      IMAGE_TYPE_NOT_ALLOWED nếu rỗng hoặc không phải ảnh thuộc định dạng được nhận
     */
    public ImageInfo validate(MultipartFile file) {
        validateSize(file.getSize());
        return readImageInfo(file);
    }

    private void validateSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw typeNotAllowed(null);
        }
        int maxSizeMb = settingService.getInt(SettingKeys.UPLOAD_IMAGE_MAX_SIZE_MB,
                StorageConstants.DEFAULT_IMAGE_MAX_SIZE_MB);
        if (sizeBytes > maxSizeMb * StorageConstants.BYTES_PER_MB) {
            throw new ApiException(ErrorCode.IMAGE_TOO_LARGE, maxSizeMb);
        }
    }

    private ImageInfo readImageInfo(MultipartFile file) {
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw typeNotAllowed(null);
            }
            return readWith(readers.next(), imageInput);
        } catch (IOException exception) {
            // Phần đầu tệp giống ảnh nhưng dữ liệu hỏng: với người dùng đây vẫn là "tệp không hợp lệ"
            throw typeNotAllowed(exception);
        }
    }

    private ImageInfo readWith(ImageReader reader, ImageInputStream imageInput) throws IOException {
        try {
            String format = reader.getFormatName().toLowerCase(Locale.ROOT);
            String extension = StorageConstants.EXTENSION_BY_IMAGE_FORMAT.get(format);
            if (extension == null) {
                throw typeNotAllowed(null);
            }
            // Chỉ đọc phần đầu để lấy kích thước, không giải mã toàn bộ ảnh
            reader.setInput(imageInput, true, true);
            return new ImageInfo(extension, reader.getWidth(0), reader.getHeight(0));
        } finally {
            reader.dispose();
        }
    }

    private static ApiException typeNotAllowed(Throwable cause) {
        return new ApiException(ErrorCode.IMAGE_TYPE_NOT_ALLOWED, cause, StorageConstants.ALLOWED_IMAGE_FORMATS_LABEL);
    }
}
