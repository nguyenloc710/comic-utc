package vn.edu.utc.comic.common.i18n;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Dịch thông báo theo ngôn ngữ của request hiện tại.
 *
 * <p>Đây là lớp DUY NHẤT trong Java được phép đọc tệp messages*.properties.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Lấy thông báo theo ngôn ngữ của người gọi hiện tại.
     *
     * @param key       khóa, lấy từ lớp MessageKeys
     * @param arguments tham số điền vào chỗ {0}, {1}...
     */
    public String getMessage(String key, Object... arguments) {
        return getMessage(key, LocaleContextHolder.getLocale(), arguments);
    }

    /** Lấy thông báo theo một ngôn ngữ chỉ định, dùng cho job chạy ngoài request. */
    public String getMessage(String key, Locale locale, Object... arguments) {
        // Không tìm thấy khóa thì trả về chính khóa đó để lỗi lộ ra ngay khi test, thay vì ném ngoại lệ
        return messageSource.getMessage(key, arguments, key, locale);
    }
}
