package vn.edu.utc.comic.chapter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import vn.edu.utc.comic.chapter.dto.ChapterReadResponse;
import vn.edu.utc.comic.chapter.event.ChapterViewedEvent;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.interaction.service.ReadingHistoryService;

/**
 * Ghi nhận việc một người vừa mở đọc một chương: tính lượt xem và lưu tiến độ đọc.
 */
@Service
@RequiredArgsConstructor
public class ChapterReadingTracker {

    private static final String USER_KEY_PREFIX = "user:";
    private static final String SESSION_KEY_PREFIX = "session:";

    private final ApplicationEventPublisher eventPublisher;
    private final ReadingHistoryService readingHistoryService;

    /**
     * Gọi sau khi đã lấy được nội dung chương cho người xem.
     *
     * <p>Không ghi nhận gì khi người xem đang xem trước chương chưa công khai, hoặc khi tác giả tự đọc
     * truyện của mình — nếu không tác giả tự tăng được lượt xem cho truyện của chính họ.
     *
     * @param sessionId id phiên, dùng để nhận ra cùng một khách vãng lai mở lại chương
     */
    public void trackRead(ChapterReadResponse chapter, Viewer viewer, String sessionId) {
        if (!chapter.publiclyReadable() || viewer.isUser(chapter.authorId())) {
            return;
        }
        String visitorKey = viewer.isAuthenticated()
                ? USER_KEY_PREFIX + viewer.userId()
                : SESSION_KEY_PREFIX + sessionId;
        // Đếm lượt xem chạy nền (listener @Async) để trang đọc không phải chờ ba câu UPDATE
        eventPublisher.publishEvent(new ChapterViewedEvent(chapter.storyId(), chapter.chapterId(), visitorKey));
        if (viewer.isAuthenticated()) {
            readingHistoryService.recordProgress(viewer.userId(), chapter.storyId(), chapter.chapterId());
        }
    }
}
