package vn.edu.utc.comic.chapter.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.chapter.event.ChapterPublishedEvent;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.notification.event.ContentHiddenEvent;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.service.StoryAccessPolicy;

/**
 * MỘT CỬA cho mọi lần đổi trạng thái chương: đăng ngay, hẹn giờ, hủy hẹn, và đăng theo giờ hẹn của job.
 *
 * <p>Lý do phải gom về đây: đăng một chương kéo theo ba việc bắt buộc đi cùng nhau — đổi trạng thái chương,
 * cộng {@code chapter_count} và cập nhật {@code last_chapter_at} của truyện, phát sự kiện để báo người theo dõi.
 * Sót một trong ba ở bất kỳ đường đi nào (nút bấm, job) là số chương hiển thị sai hoặc độc giả không được báo.
 *
 * <p>Thứ tự khóa: luôn khóa dòng truyện TRƯỚC khi ghi dòng chương, cùng thứ tự với các thao tác của độc giả
 * (xem {@code StoryRepository.lockForCounterUpdate}), để không deadlock với một bình luận đang gửi vào chương.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterPublishService {

    private static final String AUDIT_STORY_ID = "storyId";
    private static final String AUDIT_REASON = "reason";
    private static final String LABEL_SEPARATOR = " – ";
    private static final String PATH_SEPARATOR = "/";
    private static final String EDIT_SUFFIX = "/edit";

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final StudioChapterService studioChapterService;
    private final StoryAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Tác giả đăng ngay một chương đang là bản nháp hoặc đang hẹn giờ.
     *
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_INVALID_TRANSITION nếu chương đã đăng hoặc đang bị ẩn;
     *                      CHAPTER_EMPTY nếu chương chưa có nội dung
     */
    @Transactional
    public void publishNow(Long chapterId, Long authorId) {
        Chapter chapter = studioChapterService.getOwnedChapter(chapterId, authorId);
        requireTransition(chapter, ChapterStatus.PUBLISHED);
        requireContent(chapter);
        Story story = chapter.getStory();
        storyRepository.lockForCounterUpdate(story.getId());

        Instant now = clock.instant();
        chapter.setStatus(ChapterStatus.PUBLISHED);
        chapter.setPublishedAt(now);
        chapter.setScheduledAt(null);
        recordPublished(story, chapter, now);
        log.info("Tác giả {} đăng chương {}", authorId, chapterId);
    }

    /**
     * Hẹn giờ đăng (hoặc đổi giờ của chương đang hẹn).
     *
     * @param localTime giờ Việt Nam do tác giả chọn trên form
     * @return thời điểm hẹn đã đổi sang UTC
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_INVALID_TRANSITION; CHAPTER_SCHEDULE_INVALID nếu thiếu giờ
     *                      hoặc giờ không ở tương lai; CHAPTER_EMPTY nếu chương chưa có nội dung
     */
    @Transactional
    public Instant schedule(Long chapterId, Long authorId, LocalDateTime localTime) {
        Chapter chapter = studioChapterService.getOwnedChapter(chapterId, authorId);
        // Đổi giờ của chương đang hẹn không phải là một lần chuyển trạng thái
        if (chapter.getStatus() != ChapterStatus.SCHEDULED) {
            requireTransition(chapter, ChapterStatus.SCHEDULED);
        }
        Instant scheduledAt = toFutureInstant(localTime);
        requireContent(chapter);
        chapter.setStatus(ChapterStatus.SCHEDULED);
        chapter.setScheduledAt(scheduledAt);
        return scheduledAt;
    }

    /**
     * Hủy hẹn giờ, đưa chương về bản nháp.
     *
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_INVALID_TRANSITION nếu chương không ở trạng thái hẹn giờ
     */
    @Transactional
    public void cancelSchedule(Long chapterId, Long authorId) {
        Chapter chapter = studioChapterService.getOwnedChapter(chapterId, authorId);
        requireTransition(chapter, ChapterStatus.DRAFT);
        chapter.setStatus(ChapterStatus.DRAFT);
        chapter.setScheduledAt(null);
    }

    /**
     * Job đăng một chương đã tới giờ hẹn. Mỗi chương một transaction riêng: một chương lỗi không kéo các chương
     * khác rollback theo.
     *
     * <p>Việc đổi trạng thái là một câu UPDATE CÓ ĐIỀU KIỆN ({@code WHERE status = 'SCHEDULED'}), nên job chạy
     * lặp, chạy trùng trên hai máy, hay tác giả vừa bấm "Đăng ngay" / "Hủy hẹn" đúng lúc đó đều không làm chương
     * bị đăng hai lần: chỉ lời gọi nào thật sự đổi được dòng mới cộng bộ đếm và phát sự kiện.
     *
     * @return {@code true} nếu chính lời gọi này vừa đăng chương
     */
    @Transactional
    public boolean publishDueChapter(Long chapterId) {
        Chapter chapter = chapterRepository.findWithStoryById(chapterId).orElse(null);
        if (chapter == null) {
            return false;
        }
        Story story = chapter.getStory();
        storyRepository.lockForCounterUpdate(story.getId());
        Instant now = clock.instant();
        if (chapterRepository.publishScheduled(chapterId, now) == 0) {
            return false;
        }
        recordPublished(story, chapter, now);
        log.info("Job đăng chương hẹn giờ {} của truyện {}", chapterId, story.getId());
        return true;
    }

    /**
     * Quản trị viên ẩn một chương đã đăng (kèm lý do): chương biến mất với người đọc, số chương của truyện
     * giảm một, tác giả nhận thông báo kèm lý do.
     *
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_INVALID_TRANSITION nếu chương không đang đăng
     */
    @Transactional
    public void hideChapter(Long chapterId, String reason) {
        Chapter chapter = lockStoryThenLoad(chapterId);
        requireTransition(chapter, ChapterStatus.HIDDEN);
        Story story = chapter.getStory();
        chapter.setStatus(ChapterStatus.HIDDEN);
        chapter.setHiddenReason(reason.trim());
        storyRepository.addChapterCount(story.getId(), -1);
        auditService.recordForCurrentUser(AuditAction.CHAPTER_HIDDEN, AuditedEntity.of(Chapter.class, chapterId),
                Map.of(AUDIT_STORY_ID, story.getId(), AUDIT_REASON, chapter.getHiddenReason()));
        eventPublisher.publishEvent(new ContentHiddenEvent(story.getAuthor().getId(),
                chapter.getChapterNo() + LABEL_SEPARATOR + story.getTitle(), chapter.getHiddenReason(),
                ApiConstants.STUDIO_CHAPTERS_PATH + PATH_SEPARATOR + chapterId + EDIT_SUFFIX));
        log.info("Ẩn chương {}", chapterId);
    }

    /**
     * Gỡ ẩn: chương đăng trở lại, số chương cộng lại; KHÔNG phát sự kiện chương mới nên người theo dõi
     * không nhận lại thông báo.
     *
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_INVALID_TRANSITION nếu chương không đang bị ẩn
     */
    @Transactional
    public void unhideChapter(Long chapterId) {
        Chapter chapter = lockStoryThenLoad(chapterId);
        if (chapter.getStatus() != ChapterStatus.HIDDEN) {
            throw new ApiException(ErrorCode.CHAPTER_INVALID_TRANSITION);
        }
        chapter.setStatus(ChapterStatus.PUBLISHED);
        chapter.setHiddenReason(null);
        storyRepository.addChapterCount(chapter.getStory().getId(), 1);
        auditService.recordForCurrentUser(AuditAction.CHAPTER_UNHIDDEN, AuditedEntity.of(Chapter.class, chapterId),
                Map.of(AUDIT_STORY_ID, chapter.getStory().getId()));
        log.info("Gỡ ẩn chương {}", chapterId);
    }

    /** Khóa dòng truyện bằng câu lệnh đầu tiên của transaction rồi mới nạp chương (xem StoryRepository). */
    private Chapter lockStoryThenLoad(Long chapterId) {
        Long storyId = chapterRepository.findStoryIdById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAPTER_NOT_FOUND));
        storyRepository.lockForCounterUpdate(storyId);
        return chapterRepository.findWithStoryById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAPTER_NOT_FOUND));
    }
    private void recordPublished(Story story, Chapter chapter, Instant now) {
        storyRepository.recordChapterPublished(story.getId(), now);
        eventPublisher.publishEvent(new ChapterPublishedEvent(story.getId(), story.getTitle(), story.getSlug(),
                chapter.getId(), chapter.getChapterNo(), accessPolicy.canView(story, Viewer.anonymous())));
    }

    private static void requireTransition(Chapter chapter, ChapterStatus target) {
        if (!chapter.getStatus().canTransitionTo(target)) {
            throw new ApiException(ErrorCode.CHAPTER_INVALID_TRANSITION);
        }
    }

    /** Dựa vào số ảnh / số từ đã được ghi lại mỗi lần sửa nội dung, không phải nạp nội dung lên để kiểm tra. */
    private static void requireContent(Chapter chapter) {
        boolean hasContent = chapter.getStory().getType() == StoryType.COMIC
                ? chapter.getPageCount() > 0
                : chapter.getWordCount() > 0;
        if (!hasContent) {
            throw new ApiException(ErrorCode.CHAPTER_EMPTY);
        }
    }

    private Instant toFutureInstant(LocalDateTime localTime) {
        if (localTime == null) {
            throw new ApiException(ErrorCode.CHAPTER_SCHEDULE_INVALID);
        }
        Instant scheduledAt = localTime.atZone(DateTimeConstants.DISPLAY_ZONE).toInstant();
        if (!scheduledAt.isAfter(clock.instant())) {
            throw new ApiException(ErrorCode.CHAPTER_SCHEDULE_INVALID);
        }
        return scheduledAt;
    }
}
