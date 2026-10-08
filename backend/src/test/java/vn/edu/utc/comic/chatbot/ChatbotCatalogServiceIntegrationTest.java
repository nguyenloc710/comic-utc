package vn.edu.utc.comic.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.chatbot.dto.StoryDetailToolResult;
import vn.edu.utc.comic.chatbot.dto.StorySearchToolRequest;
import vn.edu.utc.comic.chatbot.dto.StorySearchToolResult;
import vn.edu.utc.comic.chatbot.dto.StoryToolItem;
import vn.edu.utc.comic.chatbot.service.ChatbotCatalogService;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;

/**
 * Các hàm tra cứu của chatbot trên MySQL thật: chỉ thấy truyện công khai, kết quả gọn, tự nới bộ lọc đúng thứ tự.
 * Mỗi test gắn một mã ngẫu nhiên vào tên truyện và tìm theo mã đó để không lẫn với dữ liệu test khác.
 */
class ChatbotCatalogServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ChatbotCatalogService chatbotCatalogService;

    @Test
    void search_neverReturnsDraftHiddenOrDeletedStories() {
        String marker = "congkhai" + uniqueSuffix();
        Story published = createStory(StoryType.NOVEL, story -> story.setTitle("Hiện " + marker));
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Nháp " + marker);
            story.setVisibility(StoryVisibility.DRAFT);
        });
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Ẩn " + marker);
            story.setVisibility(StoryVisibility.HIDDEN);
        });
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Xóa " + marker);
            story.setDeletedAt(Instant.now());
        });

        StorySearchToolResult result = chatbotCatalogService.searchStories(keyword(marker, null, null, null));

        assertThat(result.storyIds()).containsExactly(published.getId());
        assertThat(result.totalMatches()).isEqualTo(1);
        assertThat(result.relaxedFilters()).isEmpty();
    }

    @Test
    void search_returnsCompactItems_andCapsTheLimit() {
        String marker = "gonnhe" + uniqueSuffix();
        String longDescription = "Mở đầu. " + "x".repeat(500);
        for (int index = 0; index < 12; index++) {
            int number = index;
            createStory(StoryType.COMIC, story -> {
                story.setTitle("Truyện " + number + " " + marker);
                story.setDescription(longDescription);
                addGenres(story, "hanh-dong");
            });
        }

        StorySearchToolResult result = chatbotCatalogService.searchStories(
                new StorySearchToolRequest(null, null, null, null, null, null, marker, null, 50));

        // Mô hình xin 50 truyện cũng chỉ nhận tối đa 10; mô tả bị cắt để kết quả gọn
        assertThat(result.items()).hasSize(10);
        assertThat(result.totalMatches()).isEqualTo(12);
        StoryToolItem item = result.items().getFirst();
        assertThat(item.shortDescription()).hasSize(201).endsWith("…");
        assertThat(item.genres()).containsExactly("hanh-dong");
        assertThat(item.type()).isEqualTo(StoryType.COMIC);
    }

    @Test
    void search_relaxesKeywordFirst_thenChapterRange_thenStatus_andSaysSo() {
        String marker = "noiloc" + uniqueSuffix();
        // Truyện duy nhất mang thể loại hiếm kèm mã của test: đang ra, 30 chương
        Story story = createStory(StoryType.NOVEL, created -> {
            created.setTitle("Đang Ra " + marker);
            created.setStatus(StoryStatus.ONGOING);
            created.setChapterCount(30);
            addGenres(created, "am-thuc", "dien-van");
        });
        List<String> genres = List.of("am-thuc", "dien-van");

        // Xin truyện đã hoàn thành, trên 500 chương, có từ khóa không tồn tại: phải bỏ cả ba điều kiện mới ra
        StorySearchToolResult result = chatbotCatalogService.searchStories(new StorySearchToolRequest(genres, null,
                StoryType.NOVEL, StoryStatus.COMPLETED, 500, null, "khongcotukhoanay" + marker, null, null));

        assertThat(result.storyIds()).contains(story.getId());
        assertThat(result.relaxedFilters()).containsExactly("keyword", "chapterRange", "status");

        // Chỉ sai trạng thái: chỉ bỏ trạng thái, vẫn giữ từ khóa (người dùng nêu đúng tên nhưng nhầm "đã hoàn thành")
        StorySearchToolResult statusOnly = chatbotCatalogService.searchStories(new StorySearchToolRequest(genres, null,
                StoryType.NOVEL, StoryStatus.COMPLETED, null, null, marker, null, null));
        assertThat(statusOnly.storyIds()).containsExactly(story.getId());
        assertThat(statusOnly.relaxedFilters()).containsExactly("status");
    }

    @Test
    void storyDetail_ofHiddenOrUnknownStory_isNotFound_insteadOfAnError() {
        Story hidden = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.HIDDEN));
        Story published = createStory(StoryType.NOVEL, story -> story.setDescription("Mô tả đầy đủ của truyện"));

        assertThat(chatbotCatalogService.getStoryDetail(hidden.getId()).found()).isFalse();
        assertThat(chatbotCatalogService.getStoryDetail(Long.MAX_VALUE).found()).isFalse();
        assertThat(chatbotCatalogService.getStoryDetail(null).found()).isFalse();
        StoryDetailToolResult detail = chatbotCatalogService.getStoryDetail(published.getId());
        assertThat(detail.found()).isTrue();
        assertThat(detail.story().description()).isEqualTo("Mô tả đầy đủ của truyện");
        assertThat(detail.story().authorName()).isNotBlank();
    }

    @Test
    void similarStories_shareMostGenres_andExcludeTheStoryItselfAndNonPublicOnes() {
        String rare = "dien-van";
        Story source = createStory(StoryType.NOVEL, story -> addGenres(story, rare, "am-thuc", "lich-su"));
        Story closest = createStory(StoryType.NOVEL, story -> addGenres(story, rare, "am-thuc", "lich-su"));
        Story hiddenTwin = createStory(StoryType.NOVEL, story -> {
            addGenres(story, rare, "am-thuc", "lich-su");
            story.setVisibility(StoryVisibility.HIDDEN);
        });

        List<Long> similar = chatbotCatalogService.findSimilarStories(source.getId(), 10).storyIds();

        assertThat(similar).contains(closest.getId()).doesNotContain(source.getId(), hiddenTwin.getId());
        assertThat(similar.indexOf(closest.getId())).isZero();
    }

    @Test
    void fallback_recognisesGenreAndTypeNamesInTheSentence() {
        Story comicHorror = createStory(StoryType.COMIC, story -> {
            addGenres(story, "kinh-di");
            story.setViewCount(80_000_000);
        });
        Story novelHorror = createStory(StoryType.NOVEL, story -> {
            addGenres(story, "kinh-di");
            story.setViewCount(90_000_000);
        });

        List<StoryCardResponse> cards = chatbotCatalogService.searchForFallback("cho mình vài bộ Truyện Tranh KINH DỊ nhé", 6);

        assertThat(cards).extracting(StoryCardResponse::id).contains(comicHorror.getId()).doesNotContain(novelHorror.getId());
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.type()).isEqualTo(StoryType.COMIC);
            assertThat(card.genres()).extracting("slug").contains("kinh-di");
        });
        // Câu không nhắc thể loại hay loại truyện thì tìm toàn văn theo nguyên câu. Chỉ dùng từ hiếm: câu có từ phổ
        // biến ("có", "truyện") thì trên cơ sở dữ liệu dùng chung truyện khác có thể xếp trên
        String marker = "toanvan" + uniqueSuffix();
        Story byTitle = createStory(StoryType.NOVEL, story -> story.setTitle("Tên Riêng " + marker));
        assertThat(chatbotCatalogService.searchForFallback(marker, 6))
                .extracting(StoryCardResponse::id).contains(byTitle.getId());
    }

    private static StorySearchToolRequest keyword(String keyword, StoryType type, StoryStatus status, Integer limit) {
        return new StorySearchToolRequest(null, null, type, status, null, null, keyword, null, limit);
    }
}
