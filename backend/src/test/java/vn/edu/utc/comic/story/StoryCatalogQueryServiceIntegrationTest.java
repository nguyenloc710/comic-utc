package vn.edu.utc.comic.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Tìm kiếm và lọc truyện trên MySQL thật (cần FULLTEXT và collation thật nên không thể dùng cơ sở dữ liệu giả).
 * Mỗi test gắn một mã ngẫu nhiên vào tên truyện rồi tìm theo mã đó, để không lẫn với truyện của test khác.
 */
class StoryCatalogQueryServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StoryCatalogQueryService catalogQueryService;

    @Test
    void search_returnsOnlyPubliclyVisibleStories() {
        String marker = "hienthi" + uniqueSuffix();
        Story published = createStory(StoryType.NOVEL, story -> story.setTitle("Công khai " + marker));
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Bản nháp " + marker);
            story.setVisibility(StoryVisibility.DRAFT);
        });
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Bị ẩn " + marker);
            story.setVisibility(StoryVisibility.HIDDEN);
        });
        createStory(StoryType.NOVEL, story -> {
            story.setTitle("Đã xóa " + marker);
            story.setDeletedAt(Instant.now());
        });

        assertThat(search(keyword(marker))).extracting(StoryCardResponse::id).containsExactly(published.getId());
    }

    @Test
    void search_matchesTitleTypedWithoutDiacritics() {
        String marker = uniqueSuffix();
        Story story = createStory(StoryType.NOVEL, created -> created.setTitle("Tiên Lộ Vạn Cổ " + marker));

        // Tìm toàn văn khớp từng từ nên truyện khác có chung một từ cũng có thể ra; truyện khớp cả cụm đứng đầu
        assertThat(search(keyword("tien lo van co " + marker)).getFirst().id()).isEqualTo(story.getId());
        // Không có chỉ mục toàn văn nào dính vào ở đây: so khớp một phần tên cũng bỏ qua dấu
        assertThat(search(keyword("tien lo van co " + marker.substring(0, marker.length() - 1))))
                .extracting(StoryCardResponse::id).contains(story.getId());
    }

    @Test
    void search_findsWordsThatOnlyAppearInDescription() {
        String rareWord = "tuhiem" + uniqueSuffix();
        Story story = createStory(StoryType.NOVEL,
                created -> created.setDescription("Nữ chính mạnh mẽ, có biệt danh " + rareWord + " ở học viện."));

        // Từ khóa nhiều từ: chỉ cần khớp một phần là ra, truyện khớp từ hiếm đứng đầu
        List<StoryCardResponse> results = search(keyword("biệt danh " + rareWord));

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().id()).isEqualTo(story.getId());
    }

    @Test
    void search_treatsLikeWildcardsInKeywordLiterally() {
        createPublishedStory(StoryType.NOVEL);

        assertThat(search(keyword("%_%"))).isEmpty();
    }

    @Test
    void search_requiresEverySelectedGenre_andHonoursExcludedGenres() {
        String marker = "theloai" + uniqueSuffix();
        Story both = createStory(StoryType.NOVEL, story -> {
            story.setTitle("Hai thể loại " + marker);
            addGenres(story, "tu-tien", "nu-cuong");
        });
        Story single = createStory(StoryType.NOVEL, story -> {
            story.setTitle("Một thể loại " + marker);
            addGenres(story, "tu-tien");
        });

        assertThat(search(filter(marker, List.of("tu-tien"), null))).extracting(StoryCardResponse::id)
                .containsExactlyInAnyOrder(both.getId(), single.getId());
        assertThat(search(filter(marker, List.of("tu-tien", "nu-cuong"), null))).extracting(StoryCardResponse::id)
                .containsExactly(both.getId());
        assertThat(search(filter(marker, List.of("tu-tien"), List.of("nu-cuong")))).extracting(StoryCardResponse::id)
                .containsExactly(single.getId());
    }

    @Test
    void search_filtersByTypeStatusAndChapterRange() {
        String marker = "boloc" + uniqueSuffix();
        Story shortComic = createStory(StoryType.COMIC, story -> {
            story.setTitle("Tranh ngắn " + marker);
            story.setStatus(StoryStatus.COMPLETED);
            story.setChapterCount(12);
        });
        Story longNovel = createStory(StoryType.NOVEL, story -> {
            story.setTitle("Chữ dài " + marker);
            story.setStatus(StoryStatus.ONGOING);
            story.setChapterCount(300);
        });

        assertThat(search(new StoryFilterRequest(marker, null, null, StoryType.COMIC, null, null, null, null)))
                .extracting(StoryCardResponse::id).containsExactly(shortComic.getId());
        assertThat(search(new StoryFilterRequest(marker, null, null, null, StoryStatus.ONGOING, null, null, null)))
                .extracting(StoryCardResponse::id).containsExactly(longNovel.getId());
        assertThat(search(new StoryFilterRequest(marker, null, null, null, null, null, 50, null)))
                .extracting(StoryCardResponse::id).containsExactly(shortComic.getId());
        assertThat(search(new StoryFilterRequest(marker, null, null, null, null, 100, null, null)))
                .extracting(StoryCardResponse::id).containsExactly(longNovel.getId());
    }

    @Test
    void search_sortsByChosenCriterion() {
        String marker = "sapxep" + uniqueSuffix();
        Story popular = createStory(StoryType.NOVEL, story -> {
            story.setTitle("Xem nhiều " + marker);
            story.setViewCount(9_000);
            story.setRatingSum(6);
            story.setRatingCount(3);
        });
        Story acclaimed = createStory(StoryType.NOVEL, story -> {
            story.setTitle("Điểm cao " + marker);
            story.setViewCount(100);
            story.setRatingSum(10);
            story.setRatingCount(2);
        });
        Story unrated = createStory(StoryType.NOVEL, story -> story.setTitle("Chưa có điểm " + marker));

        assertThat(search(new StoryFilterRequest(marker, null, null, null, null, null, null, StorySort.VIEWS)))
                .extracting(StoryCardResponse::id)
                .containsExactly(popular.getId(), acclaimed.getId(), unrated.getId());
        // Truyện chưa có lượt đánh giá nào đứng cuối chứ không gây lỗi chia cho 0
        List<StoryCardResponse> byRating =
                search(new StoryFilterRequest(marker, null, null, null, null, null, null, StorySort.RATING));
        assertThat(byRating).extracting(StoryCardResponse::id)
                .containsExactly(acclaimed.getId(), popular.getId(), unrated.getId());
        assertThat(byRating).extracting(StoryCardResponse::ratingAverage).containsExactly(5.0, 2.0, null);
    }

    @Test
    void card_carriesGenresInDisplayOrder_andCoverUrl() {
        String marker = uniqueSuffix();
        createStory(StoryType.COMIC, story -> {
            story.setTitle("Có bìa " + marker);
            story.setCoverPath("stories/1/cover/bia.png");
            // Cố ý thêm ngược thứ tự: thẻ truyện phải theo thứ tự hiển thị của thể loại (tu-tien trước nu-cuong)
            addGenres(story, "nu-cuong", "tu-tien");
        });

        StoryCardResponse card = search(keyword(marker)).getFirst();

        assertThat(card.coverUrl()).isEqualTo("/media/stories/1/cover/bia.png");
        assertThat(card.genres()).extracting("slug").containsExactly("tu-tien", "nu-cuong");
    }

    @Test
    void findCardsByIds_keepsGivenOrder_andSkipsStoriesNoLongerPublic() {
        Story first = createPublishedStory(StoryType.NOVEL);
        Story second = createPublishedStory(StoryType.COMIC);
        Story hidden = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.HIDDEN));

        List<StoryCardResponse> cards =
                catalogQueryService.findCardsByIds(List.of(second.getId(), hidden.getId(), first.getId()));

        assertThat(cards).extracting(StoryCardResponse::id).containsExactly(second.getId(), first.getId());
    }

    @Test
    void storyDetail_ofDraft_isVisibleOnlyToItsAuthorAndAdmins() {
        Story draft = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.DRAFT));
        Viewer author = new Viewer(draft.getAuthor().getId(), Role.AUTHOR);
        Viewer otherAuthor = new Viewer(createAccount(Role.AUTHOR).getId(), Role.AUTHOR);
        Viewer admin = new Viewer(createAccount(Role.ADMIN).getId(), Role.ADMIN);

        assertThat(catalogQueryService.getStoryDetail(draft.getSlug(), author).isPreview()).isTrue();
        assertThat(catalogQueryService.getStoryDetail(draft.getSlug(), admin).isPreview()).isTrue();
        for (Viewer stranger : List.of(Viewer.anonymous(), otherAuthor)) {
            assertThatThrownBy(() -> catalogQueryService.getStoryDetail(draft.getSlug(), stranger))
                    .isInstanceOfSatisfying(ApiException.class,
                            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORY_NOT_FOUND));
        }
    }

    private List<StoryCardResponse> search(StoryFilterRequest filter) {
        return catalogQueryService.searchStories(filter, 0).content();
    }

    private static StoryFilterRequest keyword(String keyword) {
        return new StoryFilterRequest(keyword, null, null, null, null, null, null, null);
    }

    private static StoryFilterRequest filter(String keyword, List<String> genres, List<String> excludeGenres) {
        return new StoryFilterRequest(keyword, genres, excludeGenres, null, null, null, null, null);
    }
}
