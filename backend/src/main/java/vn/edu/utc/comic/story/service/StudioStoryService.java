package vn.edu.utc.comic.story.service;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.StorageConstants;
import vn.edu.utc.comic.common.constant.StoryConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.exception.FieldValidationException.FieldViolation;
import vn.edu.utc.comic.common.storage.StorageCleanup;
import vn.edu.utc.comic.common.storage.StorageService;
import vn.edu.utc.comic.common.util.SlugUtils;
import vn.edu.utc.comic.genre.dto.GenreOptionResponse;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.genre.mapper.GenreMapper;
import vn.edu.utc.comic.genre.service.GenreService;
import vn.edu.utc.comic.story.dto.StoryForm;
import vn.edu.utc.comic.story.dto.StudioStoryResponse;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.story.mapper.StoryMapper;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Quản lý truyện trong khu vực tác giả: tạo, sửa, công khai, xóa mềm (docs/00 §4.2).
 *
 * <p>Mọi thao tác đều nhận id tác giả lấy từ phiên đăng nhập và chỉ chạm được vào truyện của chính người đó;
 * truyện của người khác được trả lời như thể không tồn tại (404).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudioStoryService {

    private static final String FIELD_TITLE = "title";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_GENRES = "genreIds";
    private static final String AUDIT_TITLE = "title";
    private static final String SORT_PROPERTY = "id";
    private static final String SLUG_SEPARATOR = "-";
    /** Chừa chỗ cho hậu tố "-2", "-3"… khi tên truyện trùng nhau. */
    private static final int SLUG_SUFFIX_RESERVED_LENGTH = 6;
    private static final int FIRST_DUPLICATE_SUFFIX = 2;
    private static final String DIRECTORY_SEPARATOR = "/";

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final UserAccountRepository userAccountRepository;
    private final GenreService genreService;
    private final StoryAccessPolicy accessPolicy;
    private final StoryMapper storyMapper;
    private final GenreMapper genreMapper;
    private final StorageService storageService;
    private final StorageCleanup storageCleanup;
    private final AuditService auditService;
    private final Clock clock;

    /**
     * Truyện của một tác giả (không gồm truyện đã xóa), mới tạo trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    @Transactional(readOnly = true)
    public PageResponse<StudioStoryResponse> findStories(Long authorId, int page) {
        Page<Story> stories = storyRepository.findByAuthorIdAndDeletedAtIsNull(authorId,
                PageRequest.of(Math.max(page, 0), ApiConstants.ADMIN_PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, SORT_PROPERTY)));
        return PageResponse.of(stories, storyMapper::toStudioResponses);
    }

    /**
     * @throws ApiException STORY_NOT_FOUND nếu không có truyện hoặc truyện không phải của tác giả này
     */
    @Transactional(readOnly = true)
    public StudioStoryResponse getStory(Long storyId, Long authorId) {
        return storyMapper.toStudioResponse(getOwnedStory(storyId, authorId));
    }

    /**
     * Form sửa điền sẵn giá trị hiện tại.
     *
     * @throws ApiException STORY_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public StoryForm getForm(Long storyId, Long authorId) {
        return storyMapper.toForm(getOwnedStory(storyId, authorId));
    }

    /**
     * Thể loại chọn được trên form: các thể loại đang bật, cộng với thể loại truyện đang gắn dù đã bị tắt
     * (để lưu lại form không làm truyện tự mất thể loại).
     *
     * @param storyId truyện đang sửa; {@code null} khi tạo truyện mới
     * @throws ApiException STORY_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public List<GenreOptionResponse> findGenreOptions(Long storyId, Long authorId) {
        Set<Genre> current = storyId == null ? Set.of() : getOwnedStory(storyId, authorId).getGenres();
        return genreMapper.toOptions(genreService.findSelectableGenres(toIds(current)));
    }

    /**
     * Loại truyện (tranh / chữ) còn đổi được không: khóa lại ngay khi truyện có chương đầu tiên, vì chương
     * truyện tranh là ảnh còn chương truyện chữ là văn bản.
     *
     * @throws ApiException STORY_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public boolean isTypeLocked(Long storyId, Long authorId) {
        getOwnedStory(storyId, authorId);
        return chapterRepository.existsByStoryId(storyId);
    }

    /**
     * Tạo truyện ở trạng thái bản nháp. Slug sinh từ tên (bỏ dấu), trùng thì thêm hậu tố "-2", "-3"…
     *
     * @return id truyện vừa tạo
     * @throws FieldValidationException tên không sinh được slug, thể loại không hợp lệ
     * @throws ApiException             IMAGE_TYPE_NOT_ALLOWED / IMAGE_TOO_LARGE nếu ảnh bìa không hợp lệ — khi đó
     *                                  transaction rollback, không có truyện nào được tạo
     */
    @Transactional
    public Long createStory(Long authorId, StoryForm form) {
        Story story = new Story();
        story.setAuthor(userAccountRepository.getReferenceById(authorId));
        story.setType(form.getType());
        story.setSlug(generateSlug(form.getTitle().trim()));
        applyForm(form, story);
        storyRepository.saveAndFlush(story);
        // Thư mục ảnh bìa mang id truyện nên phải lưu truyện trước; ảnh hỏng thì cả transaction rollback
        if (hasFile(form.getCover())) {
            story.setCoverPath(storeCover(story.getId(), form.getCover()));
        }
        log.info("Tác giả {} tạo truyện {} ({})", authorId, story.getId(), story.getSlug());
        return story.getId();
    }

    /**
     * Sửa thông tin truyện. Slug giữ nguyên dù đổi tên để link cũ không chết.
     *
     * @throws ApiException             STORY_NOT_FOUND; IMAGE_TYPE_NOT_ALLOWED / IMAGE_TOO_LARGE nếu ảnh bìa mới
     *                                  không hợp lệ
     * @throws FieldValidationException đổi loại truyện khi đã có chương, thể loại không hợp lệ
     */
    @Transactional
    public void updateStory(Long storyId, Long authorId, StoryForm form) {
        Story story = getOwnedStory(storyId, authorId);
        if (form.getType() != story.getType()) {
            if (chapterRepository.existsByStoryId(storyId)) {
                throw violation(FIELD_TYPE, MessageKeys.ERROR_STORY_TYPE_LOCKED);
            }
            story.setType(form.getType());
        }
        applyForm(form, story);
        if (hasFile(form.getCover())) {
            String previousCover = story.getCoverPath();
            story.setCoverPath(storeCover(storyId, form.getCover()));
            storageCleanup.deleteAfterCommit(previousCover);
        }
    }

    /**
     * Công khai một truyện đang là bản nháp. Truyện phải đủ bìa, mô tả và ít nhất một thể loại — những thứ
     * thẻ truyện và bộ lọc cần để hiển thị đúng.
     *
     * @return tên truyện
     * @throws ApiException STORY_NOT_FOUND; STORY_VISIBILITY_NOT_CHANGEABLE nếu truyện không ở trạng thái nháp
     *                      (truyện bị quản trị viên ẩn thì tác giả không tự gỡ được);
     *                      STORY_NOT_READY_TO_PUBLISH nếu còn thiếu thông tin
     */
    @Transactional
    public String publishStory(Long storyId, Long authorId) {
        Story story = getOwnedStory(storyId, authorId);
        if (story.getVisibility() != StoryVisibility.DRAFT) {
            throw new ApiException(ErrorCode.STORY_VISIBILITY_NOT_CHANGEABLE);
        }
        if (story.getCoverPath() == null || story.getGenres().isEmpty() || story.getDescription().isBlank()) {
            throw new ApiException(ErrorCode.STORY_NOT_READY_TO_PUBLISH);
        }
        story.setVisibility(StoryVisibility.PUBLISHED);
        story.setPublishedAt(clock.instant());
        log.info("Tác giả {} công khai truyện {}", authorId, storyId);
        return story.getTitle();
    }

    /**
     * Xóa mềm truyện: biến mất với mọi người nhưng dữ liệu (chương, bình luận, ảnh) vẫn còn.
     *
     * @return tên truyện vừa xóa
     * @throws ApiException STORY_NOT_FOUND
     */
    @Transactional
    public String deleteStory(Long storyId, Long authorId) {
        Story story = getOwnedStory(storyId, authorId);
        story.setDeletedAt(clock.instant());
        auditService.recordForCurrentUser(AuditAction.STORY_DELETED, AuditedEntity.of(Story.class, storyId),
                Map.of(AUDIT_TITLE, story.getTitle()));
        log.info("Tác giả {} xóa truyện {}", authorId, storyId);
        return story.getTitle();
    }

    /**
     * Thực thể truyện mà tác giả được quản lý. Dành cho service khác trong khu vực tác giả (chương, thống kê).
     *
     * @throws ApiException STORY_NOT_FOUND — dùng chung cho "không tồn tại", "đã xóa" và "của người khác"
     */
    @Transactional(readOnly = true)
    public Story getOwnedStory(Long storyId, Long authorId) {
        return storyRepository.findById(storyId)
                .filter(story -> accessPolicy.canManage(story, authorId))
                .orElseThrow(() -> new ApiException(ErrorCode.STORY_NOT_FOUND));
    }

    private void applyForm(StoryForm form, Story story) {
        story.setTitle(form.getTitle().trim());
        story.setAltTitle(form.getAltTitle() == null || form.getAltTitle().isBlank() ? null : form.getAltTitle().trim());
        story.setDescription(form.getDescription().trim());
        story.setStatus(form.getStatus());
        Set<Genre> genres = resolveGenres(form.getGenreIds(), story.getGenres());
        story.getGenres().clear();
        story.getGenres().addAll(genres);
    }

    /** Chỉ nhận thể loại đang bật hoặc thể loại truyện vốn đã gắn; id lạ nghĩa là request bị sửa tay. */
    private Set<Genre> resolveGenres(List<Long> genreIds, Set<Genre> current) {
        Set<Long> requestedIds = genreIds == null ? Set.of() : genreIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Genre> selectable = genreService.findSelectableGenres(toIds(current)).stream()
                .collect(Collectors.toMap(Genre::getId, Function.identity()));
        if (!selectable.keySet().containsAll(requestedIds)) {
            throw violation(FIELD_GENRES, MessageKeys.ERROR_STORY_GENRE_INVALID);
        }
        return requestedIds.stream().map(selectable::get).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String generateSlug(String title) {
        String base = SlugUtils.toSlug(title, StoryConstants.SLUG_MAX_LENGTH - SLUG_SUFFIX_RESERVED_LENGTH);
        if (base.isEmpty()) {
            throw violation(FIELD_TITLE, MessageKeys.ERROR_STORY_TITLE_INVALID);
        }
        String slug = base;
        int suffix = FIRST_DUPLICATE_SUFFIX;
        // Slug của truyện đã xóa mềm vẫn bị giữ: link cũ không được trỏ sang một truyện khác
        while (storyRepository.existsBySlug(slug)) {
            slug = base + SLUG_SEPARATOR + suffix;
            suffix++;
        }
        return slug;
    }

    private String storeCover(Long storyId, MultipartFile cover) {
        String directory = StorageConstants.STORY_DIRECTORY + DIRECTORY_SEPARATOR + storyId
                + DIRECTORY_SEPARATOR + StorageConstants.COVER_DIRECTORY;
        return storageService.storeImage(cover, directory).key();
    }

    private static Set<Long> toIds(Set<Genre> genres) {
        return genres.stream().map(Genre::getId).collect(Collectors.toSet());
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static FieldValidationException violation(String field, String messageKey) {
        return new FieldValidationException(List.of(new FieldViolation(field, messageKey)));
    }
}
