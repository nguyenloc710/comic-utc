package vn.edu.utc.comic.chapter.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.dto.ChapterForm;
import vn.edu.utc.comic.chapter.dto.StudioChapterResponse;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.entity.ChapterContent;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.chapter.mapper.ChapterMapper;
import vn.edu.utc.comic.chapter.repository.ChapterContentRepository;
import vn.edu.utc.comic.chapter.repository.ChapterPageRepository;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.exception.FieldValidationException.FieldViolation;
import vn.edu.utc.comic.common.storage.StorageCleanup;
import vn.edu.utc.comic.common.util.HtmlSanitizer;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.service.StoryAccessPolicy;
import vn.edu.utc.comic.story.service.StudioStoryService;

/**
 * Soạn chương trong khu vực tác giả: tạo, sửa nội dung, xóa bản nháp.
 *
 * <p>Lớp này KHÔNG đổi trạng thái chương — đăng, hẹn giờ, hủy hẹn đi qua {@link ChapterPublishService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudioChapterService {

    private static final String FIELD_CHAPTER_NO = "chapterNo";
    private static final String FIELD_CONTENT = "contentHtml";
    private static final int FIRST_CHAPTER_NO = 1;

    private final ChapterRepository chapterRepository;
    private final ChapterContentRepository chapterContentRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final StudioStoryService studioStoryService;
    private final StoryAccessPolicy accessPolicy;
    private final ChapterMapper chapterMapper;
    private final StorageCleanup storageCleanup;

    /**
     * Mọi chương của một truyện (mọi trạng thái), số lớn trước.
     *
     * @throws ApiException STORY_NOT_FOUND nếu truyện không phải của tác giả này
     */
    @Transactional(readOnly = true)
    public List<StudioChapterResponse> findChapters(Long storyId, Long authorId) {
        studioStoryService.getOwnedStory(storyId, authorId);
        return chapterMapper.toStudioResponses(chapterRepository.findByStoryIdOrderByChapterNoDesc(storyId));
    }

    /**
     * Form trống cho chương mới, gợi ý sẵn số chương kế tiếp.
     *
     * @throws ApiException STORY_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public ChapterForm getNewForm(Long storyId, Long authorId) {
        studioStoryService.getOwnedStory(storyId, authorId);
        ChapterForm form = new ChapterForm();
        form.setChapterNo(chapterRepository.findMaxChapterNo(storyId).map(max -> max + 1).orElse(FIRST_CHAPTER_NO));
        return form;
    }

    /**
     * @throws ApiException CHAPTER_NOT_FOUND nếu không có chương hoặc chương thuộc truyện của người khác
     */
    @Transactional(readOnly = true)
    public StudioChapterResponse getChapter(Long chapterId, Long authorId) {
        return chapterMapper.toStudioResponse(getOwnedChapter(chapterId, authorId));
    }

    /**
     * Form sửa điền sẵn giá trị hiện tại; giờ hẹn được đổi về giờ Việt Nam để hiện đúng như lúc tác giả chọn.
     *
     * @throws ApiException CHAPTER_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public ChapterForm getForm(Long chapterId, Long authorId) {
        Chapter chapter = getOwnedChapter(chapterId, authorId);
        ChapterForm form = new ChapterForm();
        form.setChapterNo(chapter.getChapterNo());
        form.setTitle(chapter.getTitle());
        form.setContentHtml(chapterContentRepository.findById(chapterId).map(ChapterContent::getContent).orElse(""));
        if (chapter.getScheduledAt() != null) {
            form.setScheduledAt(LocalDateTime.ofInstant(chapter.getScheduledAt(), DateTimeConstants.DISPLAY_ZONE));
        }
        return form;
    }

    /**
     * Tạo chương ở trạng thái bản nháp. Với truyện chữ, nội dung được làm sạch rồi lưu luôn; với truyện tranh,
     * ảnh được tải lên sau qua {@link ChapterPageService}.
     *
     * @return id chương vừa tạo
     * @throws ApiException             STORY_NOT_FOUND
     * @throws FieldValidationException số chương đã có trong truyện
     */
    @Transactional
    public Long createChapter(Long storyId, Long authorId, ChapterForm form) {
        Story story = studioStoryService.getOwnedStory(storyId, authorId);
        if (chapterRepository.existsByStoryIdAndChapterNo(storyId, form.getChapterNo())) {
            throw duplicatedNumber();
        }
        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setChapterNo(form.getChapterNo());
        chapter.setTitle(normalizeTitle(form.getTitle()));
        String contentHtml = HtmlSanitizer.sanitizeNovelHtml(form.getContentHtml());
        if (story.getType() == StoryType.NOVEL) {
            chapter.setWordCount(HtmlSanitizer.countWords(contentHtml));
        }
        try {
            chapterRepository.saveAndFlush(chapter);
        } catch (DataIntegrityViolationException exception) {
            // Hai lần lưu cùng số chương tới cùng lúc: UNIQUE (story_id, chapter_no) chặn lần đến sau
            throw duplicatedNumber();
        }
        if (story.getType() == StoryType.NOVEL) {
            saveContent(chapter.getId(), contentHtml);
        }
        log.info("Tác giả {} tạo chương {} (số {}) của truyện {}", authorId, chapter.getId(), chapter.getChapterNo(),
                storyId);
        return chapter.getId();
    }

    /**
     * Sửa số, tên và (với truyện chữ) nội dung chương. Chương đã đăng vẫn sửa được nội dung nhưng không đổi
     * được số và không được để trống.
     *
     * @throws ApiException             CHAPTER_NOT_FOUND
     * @throws FieldValidationException gom mọi lỗi theo ô nhập: số chương trùng, đổi số của chương đã đăng,
     *                                  nội dung rỗng ở chương không còn là bản nháp
     */
    @Transactional
    public void updateChapter(Long chapterId, Long authorId, ChapterForm form) {
        Chapter chapter = getOwnedChapter(chapterId, authorId);
        boolean novel = chapter.getStory().getType() == StoryType.NOVEL;
        String contentHtml = HtmlSanitizer.sanitizeNovelHtml(form.getContentHtml());
        validateUpdate(chapter, form, novel, contentHtml);

        chapter.setChapterNo(form.getChapterNo());
        chapter.setTitle(normalizeTitle(form.getTitle()));
        if (novel) {
            chapter.setWordCount(HtmlSanitizer.countWords(contentHtml));
            saveContent(chapterId, contentHtml);
        }
    }

    /**
     * Xóa hẳn một chương còn là bản nháp, kèm nội dung và ảnh của nó. Chương đã hẹn giờ phải hủy hẹn trước;
     * chương đã đăng không xóa được vì bình luận và lịch sử đọc của độc giả đang trỏ tới nó.
     *
     * @return chương vừa xóa (để biết quay về truyện nào)
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_NOT_DELETABLE nếu không phải bản nháp
     */
    @Transactional
    public StudioChapterResponse deleteDraftChapter(Long chapterId, Long authorId) {
        Chapter chapter = getOwnedChapter(chapterId, authorId);
        if (chapter.getStatus() != ChapterStatus.DRAFT) {
            throw new ApiException(ErrorCode.CHAPTER_NOT_DELETABLE);
        }
        StudioChapterResponse deleted = chapterMapper.toStudioResponse(chapter);
        // Không nạp thực thể trang ở đây: Hibernate sẽ thấy chúng còn trỏ tới chương vừa bị xóa và từ chối flush
        List<String> imageKeys = chapterPageRepository.findImagePathsByChapterId(chapterId);
        chapterPageRepository.deleteByChapterId(chapterId);
        chapterContentRepository.deleteById(chapterId);
        chapterRepository.delete(chapter);
        storageCleanup.deleteAfterCommit(imageKeys);
        log.info("Tác giả {} xóa chương nháp {}", authorId, chapterId);
        return deleted;
    }

    /**
     * Thực thể chương (kèm truyện) mà tác giả được quản lý. Dành cho service khác trong khu vực tác giả.
     *
     * @throws ApiException CHAPTER_NOT_FOUND — dùng chung cho "không tồn tại" và "của người khác"
     */
    @Transactional(readOnly = true)
    public Chapter getOwnedChapter(Long chapterId, Long authorId) {
        return chapterRepository.findWithStoryById(chapterId)
                .filter(chapter -> accessPolicy.canManage(chapter.getStory(), authorId))
                .orElseThrow(() -> new ApiException(ErrorCode.CHAPTER_NOT_FOUND));
    }

    private void validateUpdate(Chapter chapter, ChapterForm form, boolean novel, String contentHtml) {
        List<FieldViolation> violations = new ArrayList<>();
        if (form.getChapterNo() != chapter.getChapterNo()) {
            if (chapter.getStatus().hasBeenPublished()) {
                violations.add(new FieldViolation(FIELD_CHAPTER_NO, MessageKeys.ERROR_CHAPTER_NO_LOCKED));
            } else if (chapterRepository.existsByStoryIdAndChapterNo(chapter.getStory().getId(), form.getChapterNo())) {
                violations.add(new FieldViolation(FIELD_CHAPTER_NO, MessageKeys.ERROR_CHAPTER_NO_DUPLICATED));
            }
        }
        // Chương đang hẹn giờ hoặc đã đăng mà bị lưu rỗng thì người đọc sẽ mở ra một trang trắng
        if (novel && chapter.getStatus() != ChapterStatus.DRAFT && HtmlSanitizer.toPlainText(contentHtml).isEmpty()) {
            violations.add(new FieldViolation(FIELD_CONTENT, MessageKeys.ERROR_CHAPTER_CONTENT_REQUIRED));
        }
        if (!violations.isEmpty()) {
            throw new FieldValidationException(violations);
        }
    }

    private void saveContent(Long chapterId, String contentHtml) {
        ChapterContent content = chapterContentRepository.findById(chapterId).orElseGet(ChapterContent::new);
        content.setChapterId(chapterId);
        content.setContent(contentHtml);
        chapterContentRepository.save(content);
    }

    private static String normalizeTitle(String title) {
        return title == null || title.isBlank() ? null : title.trim();
    }

    private static FieldValidationException duplicatedNumber() {
        return new FieldValidationException(
                List.of(new FieldViolation(FIELD_CHAPTER_NO, MessageKeys.ERROR_CHAPTER_NO_DUPLICATED)));
    }
}
