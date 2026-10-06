package vn.edu.utc.comic.common.storage;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Cho các mapper MapStruct đổi khóa ảnh trong cơ sở dữ liệu thành URL hiển thị:
 * khai báo {@code uses = MediaUrlMapper.class} rồi {@code qualifiedByName = "mediaUrl"}.
 * Nhờ vậy không mapper hay template nào tự ghép đường dẫn ảnh.
 */
@Component
@RequiredArgsConstructor
public class MediaUrlMapper {

    public static final String MEDIA_URL = "mediaUrl";

    private final StorageService storageService;

    /** @return URL của ảnh; {@code null} khi chưa có ảnh */
    @Named(MEDIA_URL)
    public String toUrl(String storageKey) {
        return storageService.resolveUrl(storageKey);
    }
}
