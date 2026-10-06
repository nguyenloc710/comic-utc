package vn.edu.utc.comic.common.constant;

import java.time.ZoneId;

/**
 * Múi giờ hiển thị của ứng dụng.
 */
public final class DateTimeConstants {

    /** Dữ liệu lưu UTC, hiển thị và nhập giờ hẹn đăng theo giờ Việt Nam. */
    public static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateTimeConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
