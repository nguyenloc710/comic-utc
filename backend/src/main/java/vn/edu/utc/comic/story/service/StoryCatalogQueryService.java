package vn.edu.utc.comic.story.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.author.entity.AuthorProfile;
import vn.edu.utc.comic.author.repository.AuthorProfileRepository;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.dto.StoryDetailResponse;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.mapper.StoryMapper;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.repository.StorySpecification;

/**
 * Mọi truy vấn truyện phía người đọc — trang chủ, tìm kiếm, chi tiết, bảng xếp hạng, tủ truyện —
 * và (ở giai đoạn 6) các hàm tra cứu của chatbot đều đi qua đây.
 *
 * <p>Lý do gom về một chỗ: điều kiện "truyện công khai" chỉ được ghép ở lớp này, nên không trang nào hay hàm
 * nào có thể vô tình để lộ truyện nháp, truyện bị ẩn hoặc truyện đã xóa.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryCatalogQueryService {

    private final StoryRepository storyRepository;
    private final AuthorProfileRepository authorProfileRepository;
    private final StoryAccessPolicy accessPolicy;
    private final StoryMapper storyMapper;

    /**
     * Tìm và lọc truyện công khai.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    public PageResponse<StoryCardResponse> searchStories(StoryFilterRequest filter, int page) {
        // Thứ tự sắp xếp nằm trong Specification (có tiêu chí là biểu thức), nên PageRequest không mang Sort
        Page<Story> stories = storyRepository.findAll(publicMatching(filter),
                PageRequest.of(Math.max(page, 0), ApiConstants.STORY_PAGE_SIZE));
        return PageResponse.of(stories, storyMapper::toCards);
    }

    /** Một số truyện công khai đứng đầu theo bộ lọc, cho các khối của trang chủ và bảng xếp hạng. */
    public List<StoryCardResponse> findTopStories(StoryFilterRequest filter, int limit) {
        return storyMapper.toCards(storyRepository.findAll(publicMatching(filter), PageRequest.of(0, limit))
                .getContent());
    }

    /** Truyện có điểm đánh giá cao nhất, chỉ tính truyện đã đủ số lượt đánh giá tối thiểu. */
    public List<StoryCardResponse> findTopRated(StoryFilterRequest filter, int minRatingCount, int limit) {
        Specification<Story> specification = publicMatching(filter).and(StorySpecification.ratedAtLeast(minRatingCount));
        return storyMapper.toCards(storyRepository.findAll(specification, PageRequest.of(0, limit)).getContent());
    }

    /**
     * Thẻ truyện theo danh sách id, GIỮ đúng thứ tự của danh sách (dùng cho bảng xếp hạng theo lượt xem).
     * Id của truyện không còn công khai bị bỏ qua.
     */
    public List<StoryCardResponse> findCardsByIds(List<Long> storyIds) {
        if (storyIds.isEmpty()) {
            return List.of();
        }
        Specification<Story> specification = StorySpecification.publiclyVisible()
                .and((root, query, builder) -> root.get("id").in(storyIds));
        return storyRepository.findAll(specification).stream()
                .sorted(Comparator.comparingInt(story -> storyIds.indexOf(story.getId())))
                .map(storyMapper::toCard)
                .toList();
    }

    /** Truyện một người đang theo dõi; truyện đã bị ẩn hoặc xóa không còn xuất hiện. */
    public PageResponse<StoryCardResponse> findFollowedStories(Long userId, int page) {
        Page<Story> stories = storyRepository.findFollowedBy(userId,
                PageRequest.of(Math.max(page, 0), ApiConstants.STORY_PAGE_SIZE));
        return PageResponse.of(stories, storyMapper::toCards);
    }

    /**
     * Trang chi tiết truyện.
     *
     * @throws ApiException STORY_NOT_FOUND nếu không có truyện hoặc người xem không được phép xem
     */
    public StoryDetailResponse getStoryDetail(String slug, Viewer viewer) {
        Story story = getViewableStory(slug, viewer);
        return storyMapper.toDetail(story, resolveAuthorName(story));
    }

    /**
     * Thực thể truyện mà người xem được phép xem (công khai, hoặc bản xem trước của chính tác giả / quản trị viên).
     * Dành cho service khác cần đi tiếp từ truyện, ví dụ đọc chương.
     *
     * @throws ApiException STORY_NOT_FOUND — dùng chung cho "không tồn tại" và "không được xem" để không lộ bản nháp
     */
    public Story getViewableStory(String slug, Viewer viewer) {
        return storyRepository.findBySlug(slug)
                .filter(story -> accessPolicy.canView(story, viewer))
                .orElseThrow(() -> new ApiException(ErrorCode.STORY_NOT_FOUND));
    }

    /**
     * Thực thể truyện đang công khai, cho các thao tác của độc giả (theo dõi, đánh giá, bình luận):
     * những thao tác đó không áp dụng với bản xem trước.
     *
     * @throws ApiException STORY_NOT_FOUND
     */
    public Story getPublicStory(Long storyId) {
        return storyRepository.findById(storyId)
                .filter(story -> accessPolicy.canView(story, Viewer.anonymous()))
                .orElseThrow(() -> new ApiException(ErrorCode.STORY_NOT_FOUND));
    }

    /** Truyện đang công khai theo id; rỗng nếu không có hoặc không công khai (không ném lỗi). */
    public Optional<Story> findPublicStory(Long storyId) {
        return storyRepository.findById(storyId).filter(story -> accessPolicy.canView(story, Viewer.anonymous()));
    }

    /**
     * Thực thể các truyện công khai khớp bộ lọc, cho service cần tự ánh xạ sang dạng riêng (hàm tra cứu của
     * chatbot). Phải gọi trong transaction của service gọi nếu cần đọc thể loại.
     */
    public List<Story> findPublicStories(StoryFilterRequest filter, int limit) {
        return storyRepository.findAll(publicMatching(filter), PageRequest.of(0, limit)).getContent();
    }

    /** Số truyện công khai khớp bộ lọc. */
    public long countPublicStories(StoryFilterRequest filter) {
        return storyRepository.count(publicMatching(filter));
    }

    /** Thực thể truyện công khai theo danh sách id, GIỮ đúng thứ tự; id không còn công khai bị bỏ qua. */
    public List<Story> findPublicStoriesByIds(List<Long> storyIds) {
        if (storyIds.isEmpty()) {
            return List.of();
        }
        Specification<Story> specification = StorySpecification.publiclyVisible()
                .and((root, query, builder) -> root.get("id").in(storyIds));
        return storyRepository.findAll(specification).stream()
                .sorted(Comparator.comparingInt(story -> storyIds.indexOf(story.getId())))
                .toList();
    }

    /** Id các truyện công khai chung nhiều thể loại nhất với một truyện. */
    public List<Long> findSimilarStoryIds(Long storyId, int limit) {
        return storyRepository.findSimilarStoryIds(storyId, PageRequest.of(0, limit));
    }
    private static Specification<Story> publicMatching(StoryFilterRequest filter) {
        return StorySpecification.publiclyVisible().and(StorySpecification.matching(filter));
    }

    /** Bút danh trong hồ sơ tác giả; tác giả chưa có hồ sơ (dữ liệu cũ) thì dùng tên hiển thị của tài khoản. */
    private String resolveAuthorName(Story story) {
        return authorProfileRepository.findById(story.getAuthor().getId())
                .map(AuthorProfile::getPenName)
                .orElseGet(() -> story.getAuthor().getDisplayName());
    }
}
