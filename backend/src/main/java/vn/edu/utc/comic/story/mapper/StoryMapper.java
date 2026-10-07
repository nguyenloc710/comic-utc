package vn.edu.utc.comic.story.mapper;

import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.edu.utc.comic.common.storage.MediaUrlMapper;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.genre.mapper.GenreMapper;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.dto.StoryDetailResponse;
import vn.edu.utc.comic.story.dto.StoryForm;
import vn.edu.utc.comic.story.dto.StudioStoryResponse;
import vn.edu.utc.comic.story.entity.Story;

/** Ánh xạ truyện sang DTO. Phải gọi trong transaction vì đọc quan hệ lazy (thể loại). */
@Mapper(uses = {MediaUrlMapper.class, GenreMapper.class})
public interface StoryMapper {

    String RATING_AVERAGE = "ratingAverage";
    String GENRE_IDS = "genreIds";

    @Mapping(target = "coverUrl", source = "coverPath", qualifiedByName = MediaUrlMapper.MEDIA_URL)
    @Mapping(target = "ratingAverage", source = "story", qualifiedByName = RATING_AVERAGE)
    StoryCardResponse toCard(Story story);

    List<StoryCardResponse> toCards(List<Story> stories);

    /**
     * @param authorName bút danh của tác giả, do service tra từ hồ sơ tác giả
     */
    @Mapping(target = "coverUrl", source = "story.coverPath", qualifiedByName = MediaUrlMapper.MEDIA_URL)
    @Mapping(target = "ratingAverage", source = "story", qualifiedByName = RATING_AVERAGE)
    @Mapping(target = "authorId", source = "story.author.id")
    StoryDetailResponse toDetail(Story story, String authorName);

    @Mapping(target = "coverUrl", source = "coverPath", qualifiedByName = MediaUrlMapper.MEDIA_URL)
    @Mapping(target = "ratingAverage", source = "story", qualifiedByName = RATING_AVERAGE)
    StudioStoryResponse toStudioResponse(Story story);

    List<StudioStoryResponse> toStudioResponses(List<Story> stories);

    /** Form sửa truyện điền sẵn; ô chọn ảnh bìa luôn để trống (bỏ trống nghĩa là giữ bìa hiện tại). */
    @Mapping(target = "genreIds", source = "genres", qualifiedByName = GENRE_IDS)
    @Mapping(target = "cover", ignore = true)
    StoryForm toForm(Story story);

    /** Điểm trung bình tính từ tổng sao và số lượt; không lưu cột số thực để khỏi lệch do làm tròn. */
    @Named(RATING_AVERAGE)
    default Double toRatingAverage(Story story) {
        return story.getRatingCount() == 0 ? null : (double) story.getRatingSum() / story.getRatingCount();
    }

    @Named(GENRE_IDS)
    default List<Long> toGenreIds(Set<Genre> genres) {
        return genres.stream().map(Genre::getId).toList();
    }
}
