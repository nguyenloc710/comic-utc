package vn.edu.utc.comic.chapter.job;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.chapter.service.ChapterPublishService;
import vn.edu.utc.comic.common.constant.ChapterConstants;
import vn.edu.utc.comic.common.job.JobRun;
import vn.edu.utc.comic.common.job.JobRunRepository;
import vn.edu.utc.comic.common.job.JobStatus;

/**
 * Đăng các chương hẹn giờ đã tới giờ. Chạy lặp lại sau mỗi khoảng nghỉ cấu hình ở application.yml.
 *
 * <p>Job chỉ tìm chương tới hạn rồi gọi {@link ChapterPublishService#publishDueChapter} cho TỪNG chương —
 * mỗi chương một transaction, nên một chương lỗi không chặn các chương còn lại. Tính lặp lại an toàn
 * (idempotent) nằm ở service: chạy trùng hay chạy lại đều không đăng một chương hai lần.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChapterPublishJob {

    private final ChapterRepository chapterRepository;
    private final ChapterPublishService chapterPublishService;
    private final JobRunRepository jobRunRepository;
    private final Clock clock;

    /** Điểm vào của bộ lập lịch. */
    @Scheduled(fixedDelayString = "${app.job.chapter-publish.fixed-delay}",
            initialDelayString = "${app.job.chapter-publish.initial-delay}")
    public void runScheduled() {
        publishDueChapters();
    }

    /**
     * Đăng mọi chương hẹn giờ đã tới hạn (tối đa một lô mỗi lượt; phần còn lại để lượt sau).
     *
     * @return số chương vừa được đăng trong lượt này
     */
    public int publishDueChapters() {
        Instant startedAt = clock.instant();
        List<Long> dueChapterIds = chapterRepository.findDueScheduledIds(startedAt,
                PageRequest.of(0, ChapterConstants.PUBLISH_JOB_BATCH_SIZE));
        // Lượt không có gì để đăng thì không ghi job_run: job chạy mỗi phút, ghi hết sẽ là 1.440 dòng rỗng mỗi ngày
        if (dueChapterIds.isEmpty()) {
            return 0;
        }
        int publishedCount = 0;
        int errorCount = 0;
        String lastError = null;
        for (Long chapterId : dueChapterIds) {
            try {
                if (chapterPublishService.publishDueChapter(chapterId)) {
                    publishedCount++;
                }
            } catch (RuntimeException exception) {
                errorCount++;
                lastError = exception.toString();
                log.error("Không đăng được chương hẹn giờ {}", chapterId, exception);
            }
        }
        recordRun(startedAt, publishedCount, errorCount, lastError);
        return publishedCount;
    }

    private void recordRun(Instant startedAt, int publishedCount, int errorCount, String lastError) {
        JobRun run = new JobRun();
        run.setJobName(ChapterConstants.PUBLISH_JOB_NAME);
        run.setStartedAt(startedAt);
        run.setFinishedAt(clock.instant());
        run.setStatus(errorCount == 0 ? JobStatus.SUCCESS : JobStatus.FAILED);
        run.setProcessedCount(publishedCount);
        run.setErrorCount(errorCount);
        run.setErrorMessage(truncate(lastError));
        jobRunRepository.save(run);
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= ChapterConstants.JOB_ERROR_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ChapterConstants.JOB_ERROR_MESSAGE_MAX_LENGTH);
    }
}
