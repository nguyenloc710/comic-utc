package vn.edu.utc.comic.chatbot.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chatbot.dto.StoryDetailToolResult;
import vn.edu.utc.comic.chatbot.dto.StoryListToolResult;
import vn.edu.utc.comic.chatbot.dto.StorySearchToolRequest;
import vn.edu.utc.comic.chatbot.dto.StorySearchToolResult;
import vn.edu.utc.comic.chatbot.enums.TrendingPeriod;
import vn.edu.utc.comic.chatbot.mapper.ChatMapper;
import vn.edu.utc.comic.chatbot.tool.ChatToolDescriptions;
import vn.edu.utc.comic.common.constant.ChatbotConstants;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.common.util.SlugUtils;
import vn.edu.utc.comic.genre.dto.GenrePromptItem;
import vn.edu.utc.comic.genre.service.GenreService;
import vn.edu.utc.comic.stats.service.RankingService;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Phần nghiệp vụ của các hàm tra cứu chatbot. Mọi truy vấn đi qua {@link StoryCatalogQueryService} — nơi duy
 * nhất ghép điều kiện "truyện công khai" — nên kết quả không bao giờ chứa truyện nháp, bị ẩn hay đã xóa.
 *
 * <p>Tách khỏi lớp {@code @Tool} để ánh xạ thể loại (quan hệ lazy) chạy trong transaction và để kiểm thử được
 * mà không cần mô hình.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotCatalogService {

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final RankingService rankingService;
    private final ChatMapper chatMapper;
    private final GenreService genreService;

    /** So khớp theo slug bỏ dấu, có gạch ngang hai đầu để "do-thi" không khớp nhầm giữa một từ khác. */
    private static final String SLUG_BOUNDARY = "-";
    private static final String COMIC_WORDS = "-truyen-tranh-";
    private static final String NOVEL_WORDS = "-truyen-chu-";

    /**
     * Tìm truyện theo bộ lọc mô hình điền. Không có kết quả thì nới dần theo thứ tự bỏ từ khóa → bỏ khoảng số
     * chương → bỏ trạng thái cho tới khi có kết quả, rồi TRẢ LẠI những điều kiện hóa ra không cần bỏ (người dùng
     * nêu tên truyện nhưng nhầm trạng thái thì chỉ bỏ trạng thái, vẫn giữ tên). Báo lại những gì đã bỏ để mô hình
     * nói thật với người dùng. Làm ở máy chủ thay vì để mô hình tự thử lại: tiết kiệm vòng gọi mô hình, hành vi ổn
     * định và kiểm thử được.
     */
    public StorySearchToolResult searchStories(StorySearchToolRequest searchRequest) {
        // Mô hình gọi hàm không kèm tham số nào thì coi như tìm không lọc
        StorySearchToolRequest request = searchRequest == null
                ? new StorySearchToolRequest(null, null, null, null, null, null, null, null, null)
                : searchRequest;
        StoryFilterRequest original = toFilter(request);
        EnumSet<Relaxation> dropped = EnumSet.noneOf(Relaxation.class);
        long total = storyCatalogQueryService.countPublicStories(original);
        for (Relaxation relaxation : Relaxation.values()) {
            if (total > 0) {
                break;
            }
            if (relaxation.appliesTo(original)) {
                dropped.add(relaxation);
                total = storyCatalogQueryService.countPublicStories(Relaxation.apply(original, dropped));
            }
        }
        if (total > 0 && dropped.size() > 1) {
            total = restoreUnneeded(original, dropped, total);
        }
        StoryFilterRequest effective = Relaxation.apply(original, dropped);
        List<Story> stories = total == 0 ? List.of()
                : storyCatalogQueryService.findPublicStories(effective, normalizeLimit(request.limit()));
        List<String> relaxed = dropped.stream().map(Relaxation::getName).toList();
        return new StorySearchToolResult(chatMapper.toToolItems(stories), total, relaxed);
    }

    /** Thử thêm lại từng điều kiện đã bỏ (trừ điều kiện bỏ sau cùng); vẫn có kết quả thì giữ điều kiện đó. */
    private long restoreUnneeded(StoryFilterRequest original, EnumSet<Relaxation> dropped, long total) {
        long current = total;
        List<Relaxation> candidates = new ArrayList<>(dropped);
        candidates.removeLast();
        for (Relaxation candidate : candidates) {
            EnumSet<Relaxation> withoutCandidate = EnumSet.copyOf(dropped);
            withoutCandidate.remove(candidate);
            long count = storyCatalogQueryService.countPublicStories(Relaxation.apply(original, withoutCandidate));
            if (count > 0) {
                dropped.remove(candidate);
                current = count;
            }
        }
        return current;
    }

    /** Chi tiết một truyện đang công khai; id không có hoặc không công khai thì {@code found = false}. */
    public StoryDetailToolResult getStoryDetail(Long storyId) {
        // Không ném lỗi cho mô hình: một id sai là chuyện bình thường, mô hình tự tìm lại là được. Cũng không
        // bắt ApiException ở đây — lỗi đi qua proxy giao dịch sẽ đánh dấu cả transaction phải rollback
        return storyId == null ? StoryDetailToolResult.notFound() : storyCatalogQueryService.findPublicStory(storyId)
                .map(story -> new StoryDetailToolResult(true, chatMapper.toToolDetail(
                        storyCatalogQueryService.getStoryDetail(story.getSlug(), Viewer.anonymous()))))
                .orElseGet(StoryDetailToolResult::notFound);
    }

    /** Truyện chung nhiều thể loại nhất với một truyện công khai. */
    public StoryListToolResult findSimilarStories(Long storyId, Integer limit) {
        if (storyId == null) {
            return new StoryListToolResult(List.of());
        }
        List<Long> ids = storyCatalogQueryService.findSimilarStoryIds(storyId, normalizeLimit(limit));
        return new StoryListToolResult(chatMapper.toToolItems(storyCatalogQueryService.findPublicStoriesByIds(ids)));
    }

    /** Truyện được xem nhiều nhất trong kỳ, lấy lại bảng xếp hạng có sẵn (đã cache) rồi lọc theo loại. */
    public StoryListToolResult findTrendingStories(TrendingPeriod period, StoryType type, Integer limit) {
        TrendingPeriod effectivePeriod = period == null ? TrendingPeriod.WEEK : period;
        List<Long> ids = rankingService.getRanking(effectivePeriod.getRankingType()).stream()
                .filter(card -> type == null || card.type() == type)
                .limit(normalizeLimit(limit))
                .map(StoryCardResponse::id)
                .toList();
        return new StoryListToolResult(chatMapper.toToolItems(storyCatalogQueryService.findPublicStoriesByIds(ids)));
    }

    /**
     * Đường lui khi không có mô hình: nhận ra tên thể loại ("kinh dị") và loại truyện ("truyện tranh") có trong câu
     * — tìm toàn văn không biết đó là thể loại nên sẽ trả truyện chẳng liên quan — và lọc theo chúng, truyện nhiều
     * lượt xem trước. Câu không nhắc thể loại hay loại nào thì tìm toàn văn theo nguyên câu.
     */
    public List<StoryCardResponse> searchForFallback(String content, int limit) {
        String normalized = SLUG_BOUNDARY + SlugUtils.toSlug(content, Integer.MAX_VALUE) + SLUG_BOUNDARY;
        List<String> genres = genreService.findPromptGenres().stream()
                .map(GenrePromptItem::slug)
                .filter(slug -> normalized.contains(SLUG_BOUNDARY + slug + SLUG_BOUNDARY))
                .toList();
        StoryType type = normalized.contains(COMIC_WORDS) ? StoryType.COMIC
                : normalized.contains(NOVEL_WORDS) ? StoryType.NOVEL : null;
        if (!genres.isEmpty() || type != null) {
            List<StoryCardResponse> stories = storyCatalogQueryService.findTopStories(
                    new StoryFilterRequest(null, genres, null, type, null, null, null, StorySort.VIEWS), limit);
            if (!stories.isEmpty()) {
                return stories;
            }
        }
        return storyCatalogQueryService.findTopStories(
                new StoryFilterRequest(content, null, null, null, null, null, null, null), limit);
    }

    /** Mô hình xin bao nhiêu cũng chỉ trả tối đa {@link ChatbotConstants#TOOL_MAX_LIMIT} truyện. */
    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return ChatbotConstants.TOOL_DEFAULT_LIMIT;
        }
        return Math.min(limit, ChatbotConstants.TOOL_MAX_LIMIT);
    }

    private static StoryFilterRequest toFilter(StorySearchToolRequest request) {
        return new StoryFilterRequest(request.keyword(), request.genreSlugs(), request.excludeGenreSlugs(),
                request.type(), request.status(), request.minChapters(), request.maxChapters(), request.sortBy());
    }

    /** Các điều kiện máy chủ được phép nới khi tìm không ra, theo đúng thứ tự thử. */
    @Getter
    @RequiredArgsConstructor
    private enum Relaxation {
        KEYWORD(ChatToolDescriptions.RELAXED_KEYWORD),
        CHAPTER_RANGE(ChatToolDescriptions.RELAXED_CHAPTER_RANGE),
        STATUS(ChatToolDescriptions.RELAXED_STATUS);

        private final String name;

        boolean appliesTo(StoryFilterRequest filter) {
            return switch (this) {
                case KEYWORD -> filter.hasKeyword();
                case CHAPTER_RANGE -> filter.minChapters() != null || filter.maxChapters() != null;
                case STATUS -> filter.status() != null;
            };
        }

        /** Bộ lọc gốc sau khi bỏ các điều kiện cho trước. */
        static StoryFilterRequest apply(StoryFilterRequest filter, Set<Relaxation> dropped) {
            boolean dropRange = dropped.contains(CHAPTER_RANGE);
            return new StoryFilterRequest(
                    dropped.contains(KEYWORD) ? null : filter.keyword(),
                    filter.genres(), filter.excludeGenres(), filter.type(),
                    dropped.contains(STATUS) ? null : filter.status(),
                    dropRange ? null : filter.minChapters(),
                    dropRange ? null : filter.maxChapters(),
                    filter.sort());
        }
    }
}
