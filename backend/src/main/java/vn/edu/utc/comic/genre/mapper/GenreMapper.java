package vn.edu.utc.comic.genre.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.edu.utc.comic.genre.dto.GenreForm;
import vn.edu.utc.comic.genre.dto.GenreOptionResponse;
import vn.edu.utc.comic.genre.dto.GenreResponse;
import vn.edu.utc.comic.genre.dto.GenreTagResponse;
import vn.edu.utc.comic.genre.entity.Genre;

/** Ánh xạ thể loại. */
@Mapper
public interface GenreMapper {

    GenreResponse toResponse(Genre genre, long storyCount);

    GenreForm toForm(Genre genre);

    GenreTagResponse toTag(Genre genre);

    List<GenreTagResponse> toTags(List<Genre> genres);

    GenreOptionResponse toOption(Genre genre);

    List<GenreOptionResponse> toOptions(List<Genre> genres);

    /** Tên (đã chuẩn hóa) và slug do service quyết định, không lấy thẳng từ form. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateFromForm(GenreForm form, @MappingTarget Genre genre);
}
