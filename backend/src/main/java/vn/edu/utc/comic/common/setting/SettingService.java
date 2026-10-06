package vn.edu.utc.comic.common.setting;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.constant.CacheConstants;

/**
 * Đọc tham số vận hành từ cơ sở dữ liệu.
 *
 * <p>Giá trị được cache vì gần như không đổi nhưng được đọc ở rất nhiều request (tải ảnh, bình luận, chat).
 * Thiếu khóa hoặc giá trị hỏng thì lùi về mặc định và ghi cảnh báo, thay vì làm hỏng nghiệp vụ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingRepository settingRepository;

    @Cacheable(value = CacheConstants.SETTINGS, key = "'int:' + #key")
    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        return findRawValue(key).map(value -> parseInt(key, value, defaultValue)).orElse(defaultValue);
    }

    @Cacheable(value = CacheConstants.SETTINGS, key = "'string:' + #key")
    @Transactional(readOnly = true)
    public String getString(String key, String defaultValue) {
        return findRawValue(key).orElse(defaultValue);
    }

    @Cacheable(value = CacheConstants.SETTINGS, key = "'bool:' + #key")
    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        return findRawValue(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    /** Xóa cache sau khi quản trị viên đổi cấu hình. */
    @CacheEvict(value = CacheConstants.SETTINGS, allEntries = true)
    public void clearCache() {
        log.info("Đã xóa cache tham số vận hành");
    }

    private Optional<String> findRawValue(String key) {
        return settingRepository.findById(key).map(Setting::getSettingValue);
    }

    private int parseInt(String key, String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            log.warn("Tham số {} có giá trị {} không phải số nguyên, tạm dùng {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
