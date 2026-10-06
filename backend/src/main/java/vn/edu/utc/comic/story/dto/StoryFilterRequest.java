package vn.edu.utc.comic.story.dto;

import java.util.List;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Bộ lọc danh sách truyện công khai. Dùng chung cho trang tìm kiếm (đọc từ query string) và, ở giai đoạn 6,
 * cho hàm tìm truyện của chatbot — vì vậy có cả những tiêu chí giao diện chưa hiện ra.
 * Trường để trống nghĩa là không lọc theo trường đó.
 *
 * @param keyword       từ khóa tìm trong tên, tên khác và mô tả
 * @param genres        slug các thể loại truyện PHẢI có đủ
 * @param excludeGenres slug các thể loại truyện KHÔNG được có
 * @param minChapters   số chương đã đăng tối thiểu
 * @param maxChapters   số chương đã đăng tối đa
 * @param sort          tiêu chí sắp xếp; trống thì theo độ khớp (khi có từ khóa) hoặc mới cập nhật
 */
public record StoryFilterRequest(
        String keyword,
        List<String> genres,
        List<String> excludeGenres,
        StoryType type,
        StoryStatus status,
        Integer minChapters,
        Integer maxChapters,
        StorySort sort) {

    public StoryFilterRequest {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        genres = genres == null ? List.of() : List.copyOf(genres);
        excludeGenres = excludeGenres == null ? List.of() : List.copyOf(excludeGenres);
    }

    /** Không lọc gì, chỉ sắp xếp. */
    public static StoryFilterRequest sortedBy(StorySort sort) {
        return new StoryFilterRequest(null, null, null, null, null, null, null, sort);
    }

    /** Mọi truyện thuộc một thể loại, mới cập nhật trước. */
    public static StoryFilterRequest ofGenre(String genreSlug) {
        return new StoryFilterRequest(null, List.of(genreSlug), null, null, null, null, null, null);
    }

    /** Truyện ở một trạng thái sáng tác, mới cập nhật trước. */
    public static StoryFilterRequest ofStatus(StoryStatus status) {
        return new StoryFilterRequest(null, null, null, null, status, null, null, null);
    }

    public boolean hasKeyword() {
        return keyword != null;
    }
}
