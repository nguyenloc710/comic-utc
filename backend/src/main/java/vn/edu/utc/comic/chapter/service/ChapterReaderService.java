package vn.edu.utc.comic.chapter.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.dto.ChapterReadResponse;
import vn.edu.utc.comic.chapter.dto.ChapterSummaryResponse;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.entity.ChapterContent;
import vn.edu.utc.comic.chapter.mapper.ChapterMapper;
import vn.edu.utc.comic.chapter.repository.ChapterContentRepository;
import vn.edu.utc.comic.chapter.repository.ChapterPageRepository;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.service.StoryAccessPolicy;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Đọc chương phía người đọc. Cùng với StoryCatalogQueryService, đây là nơi duy nhất quyết định
 * chương nào được hiện cho ai.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChapterReaderService {

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final StoryAccessPolicy accessPolicy;
    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ChapterContentRepository chapterContentRepository;
    private final ChapterMapper chapterMapper;

    /** Danh sách chương đã đăng của một truyện, mới nhất trước. */
    public List<ChapterSummaryResponse> findPublishedChapters(Long storyId) {
        return chapterRepository.findPublishedSummaries(storyId);
    }

    /**
     * Nội dung một chương kèm chương trước / sau để điều hướng.
     *
     * <p>Chương nháp, hẹn giờ hoặc bị ẩn chỉ tác giả của truyện và quản trị viên xem trước được; nút điều
     * hướng thì luôn chỉ dẫn tới chương đã đăng.
     *
     * @throws ApiException STORY_NOT_FOUND / CHAPTER_NOT_FOUND nếu không tồn tại hoặc người xem không được phép
     */
    public ChapterReadResponse getChapterForReading(String storySlug, int chapterNo, Viewer viewer) {
        Story story = storyCatalogQueryService.getViewableStory(storySlug, viewer);
        Chapter chapter = chapterRepository.findByStoryIdAndChapterNo(story.getId(), chapterNo)
                .filter(found -> accessPolicy.canRead(found, story, viewer))
                .orElseThrow(() -> new ApiException(ErrorCode.CHAPTER_NOT_FOUND));
        ChapterReadResponse.Neighbors neighbors = new ChapterReadResponse.Neighbors(
                chapterRepository.findPreviousPublishedNo(story.getId(), chapterNo).orElse(null),
                chapterRepository.findNextPublishedNo(story.getId(), chapterNo).orElse(null));
        return chapterMapper.toReadResponse(story, chapter, loadBody(story, chapter), neighbors,
                accessPolicy.isPubliclyReadable(chapter, story));
    }

    private ChapterReadResponse.Body loadBody(Story story, Chapter chapter) {
        if (story.getType() == StoryType.COMIC) {
            return new ChapterReadResponse.Body(
                    chapterMapper.toPages(chapterPageRepository.findByChapterIdOrderByPageNoAsc(chapter.getId())),
                    null);
        }
        String contentHtml = chapterContentRepository.findById(chapter.getId())
                .map(ChapterContent::getContent)
                .orElse("");
        return new ChapterReadResponse.Body(List.of(), contentHtml);
    }
}
