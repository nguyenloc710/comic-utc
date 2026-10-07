package vn.edu.utc.comic.interaction.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ReportConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.util.StoryLinks;
import vn.edu.utc.comic.interaction.dto.AdminCommentFilterRequest;
import vn.edu.utc.comic.interaction.dto.AdminCommentResponse;
import vn.edu.utc.comic.interaction.entity.Comment;
import vn.edu.utc.comic.interaction.enums.CommentStatus;
import vn.edu.utc.comic.interaction.mapper.CommentMapper;
import vn.edu.utc.comic.interaction.repository.CommentRepository;
import vn.edu.utc.comic.notification.event.ContentHiddenEvent;
import vn.edu.utc.comic.story.repository.StoryRepository;

/**
 * Kiểm duyệt bình luận: quản trị viên ẩn (kèm lý do) hoặc gỡ ẩn. Bình luận bị ẩn vẫn giữ chỗ trong luồng
 * với dòng "Bình luận đã bị ẩn"; nội dung không rời máy chủ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentModerationService {

    private static final String AUDIT_REASON = "reason";
    private static final String AUDIT_STORY_ID = "storyId";
    private static final char ELLIPSIS = '…';

    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final CommentMapper commentMapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Bình luận theo bộ lọc, mới nhất trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCommentResponse> searchComments(AdminCommentFilterRequest filter, int page) {
        String keyword = filter.keyword() == null || filter.keyword().isBlank() ? null : filter.keyword().trim();
        Page<Comment> comments = commentRepository.findForAdmin(filter.status(), keyword,
                PageRequest.of(Math.max(page, 0), ApiConstants.ADMIN_PAGE_SIZE));
        return PageResponse.of(comments, commentMapper::toAdminResponses);
    }

    /**
     * Ẩn một bình luận đang hiển thị: trừ bộ đếm bình luận của truyện, báo cho người viết kèm lý do.
     *
     * @throws ApiException COMMENT_NOT_FOUND; COMMENT_MODERATION_INVALID nếu bình luận không đang hiển thị
     */
    @Transactional
    public void hideComment(Long commentId, String reason) {
        // Khóa dòng truyện trước mọi câu đọc (xem StoryRepository.lockForCounterUpdate)
        Long storyId = commentRepository.findStoryIdById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
        storyRepository.lockForCounterUpdate(storyId);
        Comment comment = getComment(commentId);
        if (comment.getStatus() != CommentStatus.VISIBLE) {
            throw new ApiException(ErrorCode.COMMENT_MODERATION_INVALID);
        }
        comment.setStatus(CommentStatus.HIDDEN);
        comment.setHiddenReason(reason.trim());
        storyRepository.addCommentCount(storyId, -1);
        auditService.recordForCurrentUser(AuditAction.COMMENT_HIDDEN, AuditedEntity.of(Comment.class, commentId),
                Map.of(AUDIT_STORY_ID, storyId, AUDIT_REASON, comment.getHiddenReason()));
        eventPublisher.publishEvent(new ContentHiddenEvent(comment.getUser().getId(), snippetOf(comment),
                comment.getHiddenReason(), buildLink(comment)));
        log.info("Ẩn bình luận {}", commentId);
    }

    /**
     * Gỡ ẩn: bình luận hiển thị trở lại và được tính lại vào bộ đếm.
     *
     * @throws ApiException COMMENT_NOT_FOUND; COMMENT_MODERATION_INVALID nếu bình luận không đang bị ẩn
     */
    @Transactional
    public void unhideComment(Long commentId) {
        Long storyId = commentRepository.findStoryIdById(commentId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
        storyRepository.lockForCounterUpdate(storyId);
        Comment comment = getComment(commentId);
        if (comment.getStatus() != CommentStatus.HIDDEN) {
            throw new ApiException(ErrorCode.COMMENT_MODERATION_INVALID);
        }
        comment.setStatus(CommentStatus.VISIBLE);
        comment.setHiddenReason(null);
        storyRepository.addCommentCount(storyId, 1);
        auditService.recordForCurrentUser(AuditAction.COMMENT_UNHIDDEN, AuditedEntity.of(Comment.class, commentId),
                Map.of(AUDIT_STORY_ID, storyId));
        log.info("Gỡ ẩn bình luận {}", commentId);
    }

    /** Đoạn đầu của bình luận, đủ để người viết nhận ra bình luận nào bị ẩn. */
    public static String snippetOf(Comment comment) {
        String content = comment.getContent();
        if (content.length() <= ReportConstants.COMMENT_SNIPPET_LENGTH) {
            return content;
        }
        return content.substring(0, ReportConstants.COMMENT_SNIPPET_LENGTH) + ELLIPSIS;
    }

    private static String buildLink(Comment comment) {
        String slug = comment.getStory().getSlug();
        return (comment.getChapter() == null
                ? StoryLinks.story(slug)
                : StoryLinks.chapter(slug, comment.getChapter().getChapterNo())) + StoryLinks.COMMENTS_ANCHOR;
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
