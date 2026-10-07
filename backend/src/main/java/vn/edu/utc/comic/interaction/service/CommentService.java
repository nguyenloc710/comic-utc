package vn.edu.utc.comic.interaction.service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.StoryConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.interaction.dto.CommentCreateRequest;
import vn.edu.utc.comic.interaction.dto.CommentResponse;
import vn.edu.utc.comic.common.util.StoryLinks;
import vn.edu.utc.comic.interaction.entity.Comment;
import vn.edu.utc.comic.interaction.event.CommentRepliedEvent;
import vn.edu.utc.comic.interaction.enums.CommentStatus;
import vn.edu.utc.comic.interaction.mapper.CommentMapper;
import vn.edu.utc.comic.interaction.repository.CommentRepository;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.service.StoryAccessPolicy;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Bình luận ở trang truyện và trang chương. Nội dung là text thuần, trả lời tối đa một cấp.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final UserAccountRepository userAccountRepository;
    private final StoryCatalogQueryService storyCatalogQueryService;
    private final StoryAccessPolicy accessPolicy;
    private final CommentMapper commentMapper;
    private final SettingService settingService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Một trang bình luận gốc (mới nhất trước), mỗi bình luận kèm toàn bộ câu trả lời của nó.
     *
     * <p>Bình luận bị ẩn hoặc đã xóa vẫn giữ chỗ trong luồng để các câu trả lời không mất ngữ cảnh,
     * nhưng nội dung của chúng không được trả về.
     *
     * @param chapterId chương cần lấy bình luận; {@code null} để lấy bình luận ở trang truyện
     * @throws ApiException STORY_NOT_FOUND nếu truyện không tồn tại hoặc không công khai
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> findComments(Long storyId, Long chapterId, int page, Viewer viewer) {
        storyCatalogQueryService.getPublicStory(storyId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), ApiConstants.COMMENT_PAGE_SIZE);
        Page<Comment> threads = chapterId == null
                ? commentRepository.findStoryThreads(storyId, pageable)
                : commentRepository.findChapterThreads(storyId, chapterId, pageable);
        return PageResponse.of(threads, content -> toThreadResponses(content, viewer.userId()));
    }

    /**
     * Thêm bình luận hoặc câu trả lời.
     *
     * @throws ApiException STORY_NOT_FOUND / CHAPTER_NOT_FOUND nếu nơi bình luận không tồn tại hoặc không công khai,
     *                      COMMENT_PARENT_INVALID nếu bình luận được trả lời không thuộc cùng nơi,
     *                      COMMENT_TOO_FAST nếu chưa hết thời gian chờ giữa hai bình luận
     */
    @Transactional
    public CommentResponse addComment(CommentCreateRequest request, Long userId) {
        // Khóa dòng truyện trước mọi câu đọc (xem StoryRepository.lockForCounterUpdate)
        storyRepository.lockForCounterUpdate(request.storyId());
        // Kiểm tra hết rồi mới ghi: nơi bình luận, bình luận được trả lời, thời gian chờ
        Story story = storyCatalogQueryService.getPublicStory(request.storyId());
        Chapter chapter = request.chapterId() == null ? null : getReadableChapter(request.chapterId(), story);
        Comment parent = request.parentId() == null ? null : getThreadRoot(request.parentId(), story, chapter);
        validateCooldown(userId);

        Comment comment = new Comment();
        comment.setStory(story);
        comment.setChapter(chapter);
        comment.setParent(parent);
        comment.setUser(userAccountRepository.getReferenceById(userId));
        comment.setContent(request.content().trim());
        commentRepository.saveAndFlush(comment);
        storyRepository.addCommentCount(story.getId(), 1);
        publishReplyEvent(comment, parent, story, chapter);
        return commentMapper.toResponse(comment, userId, List.of());
    }

    /**
     * Người viết tự xóa bình luận của mình. Bình luận giữ chỗ trong luồng ở trạng thái DELETED.
     *
     * @throws ApiException COMMENT_NOT_FOUND nếu không có bình luận hoặc bình luận là của người khác —
     *                      dùng chung một mã để không lộ id bình luận nào đang tồn tại
     */
    @Transactional
    public void deleteOwnComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
        // Chỉ bình luận đang hiển thị mới nằm trong comment_count; ẩn rồi hoặc xóa rồi thì không trừ lần nữa
        if (comment.getStatus() == CommentStatus.VISIBLE) {
            storyRepository.addCommentCount(comment.getStory().getId(), -1);
        }
        comment.setStatus(CommentStatus.DELETED);
    }

    private List<CommentResponse> toThreadResponses(List<Comment> threads, Long viewerId) {
        if (threads.isEmpty()) {
            return List.of();
        }
        // Một truy vấn cho câu trả lời của cả trang, thay vì một truy vấn cho mỗi bình luận gốc
        List<Long> threadIds = threads.stream().map(Comment::getId).toList();
        Map<Long, List<CommentResponse>> repliesByThreadId = commentRepository.findReplies(threadIds).stream()
                .collect(Collectors.groupingBy(reply -> reply.getParent().getId(),
                        Collectors.mapping(reply -> commentMapper.toResponse(reply, viewerId, List.of()),
                                Collectors.toList())));
        return threads.stream()
                .map(thread -> commentMapper.toResponse(thread, viewerId,
                        repliesByThreadId.getOrDefault(thread.getId(), List.of())))
                .toList();
    }

    /** Báo cho người viết bình luận gốc khi có NGƯỜI KHÁC trả lời; tự trả lời bình luận của mình thì không báo. */
    private void publishReplyEvent(Comment reply, Comment parent, Story story, Chapter chapter) {
        if (parent == null || parent.getUser().getId().equals(reply.getUser().getId())) {
            return;
        }
        String link = (chapter == null
                ? StoryLinks.story(story.getSlug())
                : StoryLinks.chapter(story.getSlug(), chapter.getChapterNo())) + StoryLinks.COMMENTS_ANCHOR;
        eventPublisher.publishEvent(new CommentRepliedEvent(parent.getUser().getId(),
                reply.getUser().getDisplayName(), story.getTitle(), link));
    }
    private Chapter getReadableChapter(Long chapterId, Story story) {
        return chapterRepository.findById(chapterId)
                .filter(chapter -> chapter.getStory().getId().equals(story.getId()))
                .filter(chapter -> accessPolicy.isPubliclyReadable(chapter, story))
                .orElseThrow(() -> new ApiException(ErrorCode.CHAPTER_NOT_FOUND));
    }

    /**
     * Bình luận gốc mà câu trả lời mới sẽ gắn vào. Trả lời một câu trả lời thì gắn vào bình luận gốc của nó,
     * nhờ vậy luồng không bao giờ sâu quá một cấp.
     */
    private Comment getThreadRoot(Long parentId, Story story, Chapter chapter) {
        Comment parent = commentRepository.findById(parentId)
                .filter(found -> found.getStory().getId().equals(story.getId()))
                .filter(found -> isSameChapter(found.getChapter(), chapter))
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_PARENT_INVALID));
        return parent.getParent() == null ? parent : parent.getParent();
    }

    private static boolean isSameChapter(Chapter first, Chapter second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.getId().equals(second.getId());
    }

    private void validateCooldown(Long userId) {
        int cooldownSeconds = settingService.getInt(SettingKeys.COMMENT_COOLDOWN_SECONDS,
                StoryConstants.DEFAULT_COMMENT_COOLDOWN_SECONDS);
        boolean tooFast = commentRepository.findLastCommentTime(userId)
                .map(lastCommentAt -> lastCommentAt.plus(Duration.ofSeconds(cooldownSeconds)).isAfter(clock.instant()))
                .orElse(false);
        if (tooFast) {
            throw new ApiException(ErrorCode.COMMENT_TOO_FAST, cooldownSeconds);
        }
    }
}
