package vn.edu.utc.comic.stats.dto;

/**
 * Các chỉ số chính trên trang tổng quan quản trị.
 *
 * @param userCount             tổng số tài khoản (mọi vai trò)
 * @param authorCount           số tài khoản vai trò tác giả
 * @param comicCount            số truyện tranh đang công khai
 * @param novelCount            số truyện chữ đang công khai
 * @param publishedChapterCount số chương đã đăng của các truyện đang công khai
 * @param viewsToday            lượt xem trong ngày hôm nay (giờ Việt Nam)
 * @param pendingAuthorRequests yêu cầu làm tác giả đang chờ duyệt
 * @param pendingReports        báo cáo vi phạm đang chờ xử lý
 */
public record AdminOverviewResponse(
        long userCount,
        long authorCount,
        long comicCount,
        long novelCount,
        long publishedChapterCount,
        long viewsToday,
        long pendingAuthorRequests,
        long pendingReports) {
}
