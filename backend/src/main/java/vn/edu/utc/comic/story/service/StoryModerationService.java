package vn.edu.utc.comic.story.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.dto.StudioChapterResponse;
import vn.edu.utc.comic.chapter.mapper.ChapterMapper;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.notification.event.ContentHiddenEvent;
import vn.edu.utc.comic.stats.service.RankingService;
import vn.edu.utc.comic.story.dto.AdminStoryFilterRequest;
import vn.edu.utc.comic.story.dto.AdminStoryResponse;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.story.mapper.StoryMapper;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.repository.StorySpecification;

/**
 * Kiểm duyệt truyện của quản trị viên: danh sách mọi truyện (kể cả nháp, ẩn, đã xóa), ẩn và gỡ ẩn.
 *
 * <p>Mô hình hậu kiểm (docs/00 §4.2): tác giả công khai truyện ngay, quản trị viên ẩn khi vi phạm — bắt buộc
 * có lý do, tác giả được báo và đọc được lý do trong studio, mọi thao tác đều vào nhật ký kiểm toán.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryModerationService {

    private static final String SORT_PROPERTY = "id";
    private static final String AUDIT_TITLE = "title";
    private static final String AUDIT_REASON = "reason";

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryMapper storyMapper;
    private final ChapterMapper chapterMapper;
    private final RankingService rankingService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Mọi truyện theo bộ lọc, mới tạo trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminStoryResponse> searchStories(AdminStoryFilterRequest filter, int page) {
        Page<Story> stories = storyRepository.findAll(StorySpecification.adminMatching(filter),
                PageRequest.of(Math.max(page, 0), ApiConstants.ADMIN_PAGE_SIZE, Sort.by(Sort.Direction.DESC, SORT_PROPERTY)));
        return PageResponse.of(stories, storyMapper::toAdminResponses);
    }

    /**
     * @throws ApiException STORY_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public AdminStoryResponse getStory(Long storyId) {
        return storyMapper.toAdminResponse(getStoryEntity(storyId));
    }

    /** Mọi chương của một truyện (mọi trạng thái), số lớn trước. */
    @Transactional(readOnly = true)
    public List<StudioChapterResponse> findChapters(Long storyId) {
        getStoryEntity(storyId);
        return chapterMapper.toStudioResponses(chapterRepository.findByStoryIdOrderByChapterNoDesc(storyId));
    }

    /**
     * Ẩn một truyện đang công khai. Tác giả nhận thông báo kèm lý do; bảng xếp hạng được dựng lại để truyện
     * không còn nằm đó tới khi cache hết hạn.
     *
     * @return tên truyện
     * @throws ApiException STORY_NOT_FOUND; STORY_MODERATION_INVALID nếu truyện không đang công khai
     */
    @Transactional
    public String hideStory(Long storyId, String reason) {
        Story story = getStoryEntity(storyId);
        if (story.getVisibility() != StoryVisibility.PUBLISHED || story.getDeletedAt() != null) {
            throw new ApiException(ErrorCode.STORY_MODERATION_INVALID);
        }
        story.setVisibility(StoryVisibility.HIDDEN);
        story.setHiddenReason(reason.trim());
        rankingService.evictAll();
        auditService.recordForCurrentUser(AuditAction.STORY_HIDDEN, AuditedEntity.of(Story.class, storyId),
                Map.of(AUDIT_TITLE, story.getTitle(), AUDIT_REASON, story.getHiddenReason()));
        eventPublisher.publishEvent(new ContentHiddenEvent(story.getAuthor().getId(), story.getTitle(),
                story.getHiddenReason(), ApiConstants.STUDIO_STORIES_PATH));
        log.info("Ẩn truyện {}", storyId);
        return story.getTitle();
    }

    /**
     * Gỡ ẩn: truyện công khai trở lại, không gửi thông báo.
     *
     * @return tên truyện
     * @throws ApiException STORY_NOT_FOUND; STORY_MODERATION_INVALID nếu truyện không đang bị ẩn
     */
    @Transactional
    public String unhideStory(Long storyId) {
        Story story = getStoryEntity(storyId);
        if (story.getVisibility() != StoryVisibility.HIDDEN || story.getDeletedAt() != null) {
            throw new ApiException(ErrorCode.STORY_MODERATION_INVALID);
        }
        story.setVisibility(StoryVisibility.PUBLISHED);
        story.setHiddenReason(null);
        auditService.recordForCurrentUser(AuditAction.STORY_UNHIDDEN, AuditedEntity.of(Story.class, storyId),
                Map.of(AUDIT_TITLE, story.getTitle()));
        log.info("Gỡ ẩn truyện {}", storyId);
        return story.getTitle();
    }

    private Story getStoryEntity(Long storyId) {
        return storyRepository.findById(storyId).orElseThrow(() -> new ApiException(ErrorCode.STORY_NOT_FOUND));
    }
}
