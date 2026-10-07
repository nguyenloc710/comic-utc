package vn.edu.utc.comic.chapter.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.comic.chapter.dto.ChapterPageResponse;
import vn.edu.utc.comic.chapter.dto.ChapterReadResponse;
import vn.edu.utc.comic.chapter.dto.StudioChapterPageResponse;
import vn.edu.utc.comic.chapter.dto.StudioChapterResponse;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.entity.ChapterPage;
import vn.edu.utc.comic.common.storage.MediaUrlMapper;
import vn.edu.utc.comic.story.entity.Story;

/** Ánh xạ chương sang DTO cho trang đọc và cho khu vực tác giả. */
@Mapper(uses = MediaUrlMapper.class)
public interface ChapterMapper {

    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = MediaUrlMapper.MEDIA_URL)
    ChapterPageResponse toPage(ChapterPage page);

    List<ChapterPageResponse> toPages(List<ChapterPage> pages);

    @Mapping(target = "imageUrl", source = "imagePath", qualifiedByName = MediaUrlMapper.MEDIA_URL)
    StudioChapterPageResponse toStudioPage(ChapterPage page);

    List<StudioChapterPageResponse> toStudioPages(List<ChapterPage> pages);

    @Mapping(target = "storyId", source = "story.id")
    StudioChapterResponse toStudioResponse(Chapter chapter);

    List<StudioChapterResponse> toStudioResponses(List<Chapter> chapters);

    /**
     * Ghép truyện, chương, phần thân và chương lân cận thành dữ liệu trang đọc. Truyện và chương có nhiều
     * thuộc tính trùng tên (id, title) nên từng trường phải chỉ rõ lấy từ đâu.
     */
    @Mapping(target = "storyId", source = "story.id")
    @Mapping(target = "storySlug", source = "story.slug")
    @Mapping(target = "storyTitle", source = "story.title")
    @Mapping(target = "storyType", source = "story.type")
    @Mapping(target = "authorId", source = "story.author.id")
    @Mapping(target = "chapterId", source = "chapter.id")
    @Mapping(target = "chapterNo", source = "chapter.chapterNo")
    @Mapping(target = "title", source = "chapter.title")
    @Mapping(target = "publishedAt", source = "chapter.publishedAt")
    @Mapping(target = "pages", source = "body.pages")
    @Mapping(target = "contentHtml", source = "body.contentHtml")
    @Mapping(target = "previousChapterNo", source = "neighbors.previousChapterNo")
    @Mapping(target = "nextChapterNo", source = "neighbors.nextChapterNo")
    @Mapping(target = "publiclyReadable", source = "publiclyReadable")
    ChapterReadResponse toReadResponse(Story story, Chapter chapter, ChapterReadResponse.Body body,
                                       ChapterReadResponse.Neighbors neighbors, boolean publiclyReadable);
}
