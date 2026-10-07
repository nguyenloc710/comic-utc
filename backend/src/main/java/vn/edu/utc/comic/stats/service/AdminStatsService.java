package vn.edu.utc.comic.stats.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;
import vn.edu.utc.comic.author.repository.AuthorRequestRepository;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.genre.repository.GenreRepository;
import vn.edu.utc.comic.report.service.ReportService;
import vn.edu.utc.comic.stats.dto.AdminChartsResponse;
import vn.edu.utc.comic.stats.dto.AdminOverviewResponse;
import vn.edu.utc.comic.stats.dto.DailyViewPoint;
import vn.edu.utc.comic.stats.dto.NamedCount;
import vn.edu.utc.comic.stats.repository.StoryViewDailyRepository;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Số liệu toàn hệ thống cho trang tổng quan quản trị.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final UserAccountRepository userAccountRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryViewDailyRepository storyViewDailyRepository;
    private final AuthorRequestRepository authorRequestRepository;
    private final GenreRepository genreRepository;
    private final ReportService reportService;
    private final StoryCatalogQueryService storyCatalogQueryService;
    private final Clock clock;

    /** Các chỉ số chính; tám truy vấn đếm nhẹ, không cache để con số luôn là hiện tại. */
    public AdminOverviewResponse getOverview() {
        return new AdminOverviewResponse(
                userAccountRepository.count(),
                userAccountRepository.countByRole(Role.AUTHOR),
                storyRepository.countPublicByType(StoryType.COMIC),
                storyRepository.countPublicByType(StoryType.NOVEL),
                chapterRepository.countPubliclyReadable(),
                storyViewDailyRepository.sumViewsOn(today()),
                authorRequestRepository.countByStatus(AuthorRequestStatus.PENDING),
                reportService.countPending());
    }

    /** Dữ liệu bốn biểu đồ của trang tổng quan. */
    public AdminChartsResponse getCharts() {
        LocalDate today = today();
        LocalDate fromDate = today.minusDays(ApiConstants.STATS_CHART_DAYS - 1L);
        List<NamedCount> topStories = storyCatalogQueryService
                .findTopStories(StoryFilterRequest.sortedBy(StorySort.VIEWS), ApiConstants.ADMIN_TOP_SIZE).stream()
                .map(story -> new NamedCount(story.title(), story.viewCount()))
                .toList();
        return new AdminChartsResponse(
                fillDays(countRegistrationsByDay(fromDate), fromDate, today),
                fillDays(toMap(storyViewDailyRepository.sumViewsByDay(fromDate)), fromDate, today),
                genreRepository.countPublicStoriesByGenre(PageRequest.of(0, ApiConstants.ADMIN_TOP_SIZE)),
                topStories);
    }

    /** Số đăng ký theo ngày tính theo giờ Việt Nam; gom trong Java vì mốc thời gian lưu UTC. */
    private Map<LocalDate, Long> countRegistrationsByDay(LocalDate fromDate) {
        Instant from = fromDate.atStartOfDay(DateTimeConstants.DISPLAY_ZONE).toInstant();
        return userAccountRepository.findRegistrationTimesSince(from).stream()
                .collect(Collectors.groupingBy(
                        createdAt -> LocalDate.ofInstant(createdAt, DateTimeConstants.DISPLAY_ZONE),
                        Collectors.counting()));
    }

    private static Map<LocalDate, Long> toMap(List<DailyViewPoint> points) {
        return points.stream().collect(Collectors.toMap(DailyViewPoint::date, DailyViewPoint::views));
    }

    /** Mỗi ngày đúng một điểm; ngày không có dữ liệu có giá trị 0 để đường biểu đồ không đứt. */
    private static List<DailyViewPoint> fillDays(Map<LocalDate, Long> valuesByDate, LocalDate from, LocalDate to) {
        return Stream.iterate(from, date -> !date.isAfter(to), date -> date.plusDays(1))
                .map(date -> new DailyViewPoint(date, valuesByDate.getOrDefault(date, 0L)))
                .toList();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(DateTimeConstants.DISPLAY_ZONE));
    }

}
