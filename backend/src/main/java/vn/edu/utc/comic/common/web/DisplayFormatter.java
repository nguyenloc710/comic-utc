package vn.edu.utc.comic.common.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.i18n.MessageService;

/**
 * Định dạng hiển thị dùng trong template qua tên bean: {@code ${@fmt.timeAgo(instant)}}, {@code ${@fmt.compact(n)}}.
 *
 * <p>Các danh sách truyện hiện thời gian tương đối ("2 giờ trước") và số rút gọn ("12,3K") để người đọc nhìn lướt
 * nhanh; câu chữ lấy từ messages.properties nên không có chuỗi hiển thị nào nằm ở đây.
 */
@Component("fmt")
@RequiredArgsConstructor
public class DisplayFormatter {

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long MINUTES_PER_HOUR = 60;
    private static final long HOURS_PER_DAY = 24;
    /** Quá chừng này ngày thì hiện ngày cụ thể: "45 ngày trước" khó hình dung hơn một ngày tháng. */
    private static final long MAX_RELATIVE_DAYS = 30;

    private final MessageService messageService;
    private final Clock clock;

    /** "Vừa xong", "5 phút trước", "3 giờ trước", "2 ngày trước", hoặc ngày dd/MM/yyyy nếu đã lâu. */
    public String timeAgo(Instant instant) {
        if (instant == null) {
            return "";
        }
        Duration elapsed = Duration.between(instant, clock.instant());
        long minutes = Math.max(elapsed.toMinutes(), 0);
        if (minutes < 1) {
            return messageService.getMessage(MessageKeys.TIME_JUST_NOW);
        }
        if (minutes < MINUTES_PER_HOUR) {
            return messageService.getMessage(MessageKeys.TIME_MINUTES_AGO, minutes);
        }
        long hours = minutes / MINUTES_PER_HOUR;
        if (hours < HOURS_PER_DAY) {
            return messageService.getMessage(MessageKeys.TIME_HOURS_AGO, hours);
        }
        long days = hours / HOURS_PER_DAY;
        if (days <= MAX_RELATIVE_DAYS) {
            return messageService.getMessage(MessageKeys.TIME_DAYS_AGO, days);
        }
        return LocalDateTime.ofInstant(instant, DateTimeConstants.DISPLAY_ZONE)
                .format(DateTimeFormatter.ofPattern(messageService.getMessage(MessageKeys.FORMAT_DATE)));
    }

    /** 950 → "950", 12345 → "12,3K", 2400000 → "2,4M" (dấu thập phân theo tiếng Việt). */
    public String compact(long value) {
        if (value < THOUSAND) {
            return String.valueOf(value);
        }
        boolean millions = value >= MILLION;
        BigDecimal scaled = BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(millions ? MILLION : THOUSAND), 1, RoundingMode.DOWN)
                .stripTrailingZeros();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(messageService.currentLocale());
        String number = new DecimalFormat("0.#", symbols).format(scaled);
        return messageService.getMessage(millions ? MessageKeys.NUMBER_MILLION : MessageKeys.NUMBER_THOUSAND, number);
    }
}
