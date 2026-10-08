package vn.edu.utc.comic.chatbot.job;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.utc.comic.chatbot.repository.ChatConversationRepository;
import vn.edu.utc.comic.chatbot.repository.ChatMessageRepository;
import vn.edu.utc.comic.common.constant.ChatbotConstants;
import vn.edu.utc.comic.common.job.JobRun;
import vn.edu.utc.comic.common.job.JobRunRepository;
import vn.edu.utc.comic.common.job.JobStatus;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;

/**
 * Xóa hội thoại chatbot không có tin nhắn mới quá số ngày lưu giữ (setting {@code chat.retention.days}): hội
 * thoại chứa lời người dùng gõ, không nên giữ mãi.
 *
 * <p>Xóa theo lô, mỗi lô một transaction, để câu DELETE không khóa bảng quá lâu khi tồn đọng nhiều.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCleanupJob {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final JobRunRepository jobRunRepository;
    private final SettingService settingService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /** Điểm vào của bộ lập lịch (giờ chạy cấu hình ở application.yml). */
    @Scheduled(cron = "${app.job.chat-cleanup.cron}", zone = "${app.job.chat-cleanup.zone}")
    public void runScheduled() {
        deleteExpiredConversations();
    }

    /** @return số hội thoại đã xóa */
    public int deleteExpiredConversations() {
        Instant startedAt = clock.instant();
        int retentionDays = settingService.getInt(SettingKeys.CHAT_RETENTION_DAYS, ChatbotConstants.DEFAULT_RETENTION_DAYS);
        Instant cutoff = startedAt.minus(Duration.ofDays(retentionDays));
        int deleted = 0;
        int batch;
        do {
            Integer count = transactionTemplate.execute(status -> deleteBatch(cutoff));
            batch = count == null ? 0 : count;
            deleted += batch;
        } while (batch == ChatbotConstants.CLEANUP_BATCH_SIZE);
        // Lượt không có gì để xóa thì không ghi job_run (job chạy mỗi ngày, ghi hết chỉ thêm dòng rỗng)
        if (deleted > 0) {
            recordRun(startedAt, deleted);
            log.info("Đã xóa {} hội thoại chatbot cũ hơn {} ngày", deleted, retentionDays);
        }
        return deleted;
    }

    private int deleteBatch(Instant cutoff) {
        List<Long> ids = conversationRepository.findIdsInactiveSince(cutoff,
                PageRequest.of(0, ChatbotConstants.CLEANUP_BATCH_SIZE));
        if (ids.isEmpty()) {
            return 0;
        }
        messageRepository.deleteByConversationIdIn(ids);
        conversationRepository.deleteByIdIn(ids);
        return ids.size();
    }

    private void recordRun(Instant startedAt, int deleted) {
        JobRun run = new JobRun();
        run.setJobName(ChatbotConstants.CLEANUP_JOB_NAME);
        run.setStartedAt(startedAt);
        run.setFinishedAt(clock.instant());
        run.setStatus(JobStatus.SUCCESS);
        run.setProcessedCount(deleted);
        jobRunRepository.save(run);
    }
}
