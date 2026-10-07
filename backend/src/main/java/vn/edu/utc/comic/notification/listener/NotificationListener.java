package vn.edu.utc.comic.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.utc.comic.author.event.AuthorRequestReviewedEvent;
import vn.edu.utc.comic.chapter.event.ChapterPublishedEvent;
import vn.edu.utc.comic.common.config.AsyncSchedulingConfig;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.util.StoryLinks;
import vn.edu.utc.comic.interaction.event.CommentRepliedEvent;
import vn.edu.utc.comic.notification.enums.NotificationType;
import vn.edu.utc.comic.notification.service.NotificationService;

/**
 * Đổi sự kiện nghiệp vụ thành thông báo.
 *
 * <p>Chạy SAU KHI transaction nghiệp vụ commit và trên luồng nền: nghiệp vụ rollback thì không có thông báo
 * "ma", và lỗi khi ghi thông báo không làm hỏng thao tác chính (duyệt tác giả, đăng chương, bình luận).
 */
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    /** Báo cho người gửi biết yêu cầu làm tác giả đã được duyệt (dẫn tới studio) hay bị từ chối (kèm lý do). */
    @Async(AsyncSchedulingConfig.EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuthorRequestReviewed(AuthorRequestReviewedEvent event) {
        if (event.approved()) {
            notificationService.notifyUser(event.userId(), NotificationType.AUTHOR_REQUEST_APPROVED,
                    ApiConstants.STUDIO_ROOT, event.penName());
        } else {
            notificationService.notifyUser(event.userId(), NotificationType.AUTHOR_REQUEST_REJECTED,
                    ApiConstants.AUTHOR_REQUEST_PATH, event.rejectReason());
        }
    }

    /** Báo chương mới cho mọi người theo dõi; truyện chưa công khai hoặc đang bị ẩn thì không báo. */
    @Async(AsyncSchedulingConfig.EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChapterPublished(ChapterPublishedEvent event) {
        if (!event.storyPublic()) {
            return;
        }
        notificationService.notifyFollowers(event.storyId(), NotificationType.NEW_CHAPTER,
                StoryLinks.chapter(event.storySlug(), event.chapterNo()),
                event.storyTitle(), String.valueOf(event.chapterNo()));
    }

    /** Báo cho người viết bình luận gốc khi có người khác trả lời. */
    @Async(AsyncSchedulingConfig.EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentReplied(CommentRepliedEvent event) {
        notificationService.notifyUser(event.recipientId(), NotificationType.COMMENT_REPLIED, event.link(),
                event.replierName(), event.storyTitle());
    }
}
