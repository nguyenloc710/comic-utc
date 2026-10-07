package vn.edu.utc.comic.stats.dto;

import java.time.LocalDate;

/**
 * Một điểm trên biểu đồ lượt xem theo ngày.
 *
 * @param date  ngày theo giờ Việt Nam
 * @param views tổng lượt xem trong ngày
 */
public record DailyViewPoint(LocalDate date, Long views) {
}
