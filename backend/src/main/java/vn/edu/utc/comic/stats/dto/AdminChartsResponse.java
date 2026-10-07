package vn.edu.utc.comic.stats.dto;

import java.util.List;

/**
 * Dữ liệu các biểu đồ của trang tổng quan quản trị, mỗi biểu đồ một danh sách.
 *
 * @param registrations số tài khoản đăng ký mới theo ngày, 30 ngày gần nhất
 * @param views         lượt xem toàn hệ thống theo ngày, 30 ngày gần nhất
 * @param genres        số truyện công khai theo thể loại (các thể loại nhiều truyện nhất)
 * @param topStories    truyện được xem nhiều nhất và lượt xem của chúng
 */
public record AdminChartsResponse(
        List<DailyViewPoint> registrations,
        List<DailyViewPoint> views,
        List<NamedCount> genres,
        List<NamedCount> topStories) {
}
