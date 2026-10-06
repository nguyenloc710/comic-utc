package vn.edu.utc.comic.story.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.genre.dto.GenreTagResponse;
import vn.edu.utc.comic.genre.service.GenreService;
import vn.edu.utc.comic.stats.enums.RankingType;
import vn.edu.utc.comic.stats.service.RankingService;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Duyệt kho truyện công khai: tìm kiếm và lọc, theo thể loại, bảng xếp hạng.
 */
@Controller
@RequiredArgsConstructor
public class StoryBrowseController {

    private static final String ATTR_GENRES = "genres";
    private static final String ATTR_TYPES = "types";
    private static final String ATTR_STATUSES = "statuses";
    private static final String ATTR_SORTS = "sorts";
    private static final String ATTR_GENRE = "genre";
    private static final String ATTR_RANKING = "ranking";
    private static final String ATTR_RANKING_TYPE = "rankingType";
    private static final String ATTR_RANKING_TYPES = "rankingTypes";

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final GenreService genreService;
    private final RankingService rankingService;

    /** Tìm kiếm và lọc truyện; bộ lọc nằm trên query string để chia sẻ được link. */
    @GetMapping(ApiConstants.STORIES_PATH)
    public String searchStories(@ModelAttribute(ViewConstants.ATTR_FILTER) StoryFilterRequest filter,
                                @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute(ViewConstants.ATTR_PAGE, storyCatalogQueryService.searchStories(filter, page));
        addFilterOptions(model);
        return ViewConstants.STORY_LIST;
    }

    /** Truyện thuộc một thể loại; thể loại không tồn tại hoặc đã tắt thì ra trang 404. */
    @GetMapping(ApiConstants.GENRES_PATH + "/{slug}")
    public String showGenre(@PathVariable String slug, @RequestParam(defaultValue = "0") int page, Model model) {
        GenreTagResponse genre = genreService.getActiveGenre(slug);
        StoryFilterRequest filter = StoryFilterRequest.ofGenre(genre.slug());
        model.addAttribute(ATTR_GENRE, genre);
        model.addAttribute(ViewConstants.ATTR_FILTER, filter);
        model.addAttribute(ViewConstants.ATTR_PAGE, storyCatalogQueryService.searchStories(filter, page));
        addFilterOptions(model);
        return ViewConstants.STORY_LIST;
    }

    /** Bảng xếp hạng theo tiêu chí được chọn; mặc định là lượt xem trong tuần. */
    @GetMapping(ApiConstants.RANKINGS_PATH)
    public String showRankings(@RequestParam(defaultValue = "WEEK") RankingType type, Model model) {
        model.addAttribute(ATTR_RANKING, rankingService.getRanking(type));
        model.addAttribute(ATTR_RANKING_TYPE, type);
        model.addAttribute(ATTR_RANKING_TYPES, RankingType.values());
        return ViewConstants.STORY_RANKINGS;
    }

    private void addFilterOptions(Model model) {
        model.addAttribute(ATTR_GENRES, genreService.findActiveGenres());
        model.addAttribute(ATTR_TYPES, StoryType.values());
        model.addAttribute(ATTR_STATUSES, StoryStatus.values());
        model.addAttribute(ATTR_SORTS, StorySort.values());
    }
}
