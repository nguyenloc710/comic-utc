package vn.edu.utc.comic.chapter.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.chapter.dto.StudioChapterPageResponse;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.entity.ChapterPage;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.chapter.mapper.ChapterMapper;
import vn.edu.utc.comic.chapter.repository.ChapterPageRepository;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.common.constant.ChapterConstants;
import vn.edu.utc.comic.common.constant.StorageConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.common.storage.StorageCleanup;
import vn.edu.utc.comic.common.storage.StorageService;
import vn.edu.utc.comic.common.storage.StoredFile;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Trang ảnh của chương truyện tranh: tải lên từng ảnh, sắp xếp lại, xóa.
 *
 * <p>Số thứ tự trang luôn liền mạch 1..n và do lớp này ghi lại sau mỗi thao tác. Mỗi thao tác ghi khóa dòng chương
 * bằng câu lệnh ĐẦU TIÊN của transaction, nên hai lượt tải ảnh đồng thời vào một chương chạy lần lượt và không
 * sinh ra hai trang cùng số. Phải là câu lệnh đầu tiên: ở mức cô lập mặc định của MySQL (REPEATABLE READ),
 * transaction chụp ảnh dữ liệu ngay ở câu SELECT thường đầu tiên — nếu đọc chương rồi mới khóa thì sau khi chờ
 * được khóa, câu đếm số trang vẫn nhìn ảnh chụp cũ và không thấy trang mà lượt kia vừa thêm.
 */
@Service
@RequiredArgsConstructor
public class ChapterPageService {

    private static final String DIRECTORY_SEPARATOR = "/";

    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final StudioChapterService studioChapterService;
    private final ChapterMapper chapterMapper;
    private final StorageService storageService;
    private final StorageCleanup storageCleanup;
    private final SettingService settingService;

    /**
     * Các trang của chương theo thứ tự.
     *
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_TYPE_MISMATCH nếu chương thuộc truyện chữ
     */
    @Transactional(readOnly = true)
    public List<StudioChapterPageResponse> findPages(Long chapterId, Long authorId) {
        getComicChapter(chapterId, authorId);
        return chapterMapper.toStudioPages(chapterPageRepository.findByChapterIdOrderByPageNoAsc(chapterId));
    }

    /**
     * Thêm một ảnh vào CUỐI chương. Trình duyệt gửi từng ảnh một request theo thứ tự tên tệp, nên thứ tự tải
     * lên chính là thứ tự trang ban đầu.
     *
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_TYPE_MISMATCH; CHAPTER_PAGE_LIMIT nếu chương đã đủ số ảnh
     *                      cho phép (đọc từ setting); IMAGE_TYPE_NOT_ALLOWED / IMAGE_TOO_LARGE nếu ảnh không hợp lệ
     */
    @Transactional
    public StudioChapterPageResponse addPage(Long chapterId, Long authorId, MultipartFile file) {
        chapterRepository.lockForPageUpdate(chapterId);
        Chapter chapter = getComicChapter(chapterId, authorId);
        int currentCount = chapterPageRepository.countByChapterId(chapterId);
        int maxPages = settingService.getInt(SettingKeys.UPLOAD_CHAPTER_MAX_PAGES, ChapterConstants.DEFAULT_MAX_PAGES);
        if (currentCount >= maxPages) {
            throw new ApiException(ErrorCode.CHAPTER_PAGE_LIMIT, maxPages);
        }
        StoredFile stored = storageService.storeImage(file, buildDirectory(chapter));

        ChapterPage page = new ChapterPage();
        page.setChapter(chapter);
        page.setPageNo(currentCount + 1);
        page.setImagePath(stored.key());
        page.setWidth(stored.width());
        page.setHeight(stored.height());
        page.setSizeBytes(stored.sizeBytes());
        chapterPageRepository.save(page);
        chapterRepository.updatePageCount(chapterId, currentCount + 1);
        return chapterMapper.toStudioPage(page);
    }

    /**
     * Xóa một trang rồi đánh số lại các trang còn lại.
     *
     * @return các trang còn lại theo thứ tự mới
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_TYPE_MISMATCH; CHAPTER_PAGE_NOT_FOUND;
     *                      CHAPTER_EMPTY nếu đó là trang cuối cùng của một chương đang hẹn giờ hoặc đã đăng
     */
    @Transactional
    public List<StudioChapterPageResponse> deletePage(Long chapterId, Long pageId, Long authorId) {
        chapterRepository.lockForPageUpdate(chapterId);
        Chapter chapter = getComicChapter(chapterId, authorId);
        List<ChapterPage> pages = new ArrayList<>(chapterPageRepository.findByChapterIdOrderByPageNoAsc(chapterId));
        ChapterPage target = pages.stream()
                .filter(page -> page.getId().equals(pageId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.CHAPTER_PAGE_NOT_FOUND));
        // Chương đang hẹn giờ hoặc đã đăng mà hết ảnh thì người đọc sẽ mở ra một trang trắng
        if (pages.size() == 1 && chapter.getStatus() != ChapterStatus.DRAFT) {
            throw new ApiException(ErrorCode.CHAPTER_EMPTY);
        }
        pages.remove(target);
        chapterPageRepository.delete(target);
        renumber(pages);
        chapterRepository.updatePageCount(chapterId, pages.size());
        storageCleanup.deleteAfterCommit(target.getImagePath());
        return chapterMapper.toStudioPages(pages);
    }

    /**
     * Sắp xếp lại các trang theo danh sách id do trình duyệt gửi sau khi kéo thả.
     *
     * @param orderedPageIds id của mọi trang trong chương theo thứ tự mới
     * @return các trang theo thứ tự mới
     * @throws ApiException CHAPTER_NOT_FOUND; CHAPTER_TYPE_MISMATCH; CHAPTER_PAGE_ORDER_INVALID nếu danh sách
     *                      không khớp đúng tập trang hiện có (thiếu, thừa, lặp, hoặc trang của chương khác)
     */
    @Transactional
    public List<StudioChapterPageResponse> reorderPages(Long chapterId, Long authorId, List<Long> orderedPageIds) {
        chapterRepository.lockForPageUpdate(chapterId);
        getComicChapter(chapterId, authorId);
        Map<Long, ChapterPage> pagesById = chapterPageRepository.findByChapterIdOrderByPageNoAsc(chapterId).stream()
                .collect(Collectors.toMap(ChapterPage::getId, Function.identity()));
        if (orderedPageIds.size() != pagesById.size() || !pagesById.keySet().equals(new HashSet<>(orderedPageIds))) {
            throw new ApiException(ErrorCode.CHAPTER_PAGE_ORDER_INVALID);
        }
        List<ChapterPage> ordered = orderedPageIds.stream().map(pagesById::get).toList();
        renumber(ordered);
        return chapterMapper.toStudioPages(ordered);
    }

    private static void renumber(List<ChapterPage> pagesInOrder) {
        for (int index = 0; index < pagesInOrder.size(); index++) {
            pagesInOrder.get(index).setPageNo(index + 1);
        }
    }

    private Chapter getComicChapter(Long chapterId, Long authorId) {
        Chapter chapter = studioChapterService.getOwnedChapter(chapterId, authorId);
        if (chapter.getStory().getType() != StoryType.COMIC) {
            throw new ApiException(ErrorCode.CHAPTER_TYPE_MISMATCH);
        }
        return chapter;
    }

    /** stories/{storyId}/chapters/{chapterId}: gom ảnh theo chương để dễ dọn khi cần. */
    private static String buildDirectory(Chapter chapter) {
        return StorageConstants.STORY_DIRECTORY + DIRECTORY_SEPARATOR + chapter.getStory().getId()
                + DIRECTORY_SEPARATOR + StorageConstants.CHAPTER_DIRECTORY + DIRECTORY_SEPARATOR + chapter.getId();
    }
}
