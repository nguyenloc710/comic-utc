package vn.edu.utc.comic.chatbot.mapper;

import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.edu.utc.comic.chatbot.dto.ChatCardResponse;
import vn.edu.utc.comic.chatbot.dto.ChatConversationResponse;
import vn.edu.utc.comic.chatbot.dto.ChatHistoryEntry;
import vn.edu.utc.comic.chatbot.dto.ChatMessageResponse;
import vn.edu.utc.comic.chatbot.dto.StoryToolDetail;
import vn.edu.utc.comic.chatbot.dto.StoryToolItem;
import vn.edu.utc.comic.chatbot.entity.ChatConversation;
import vn.edu.utc.comic.chatbot.entity.ChatMessage;
import vn.edu.utc.comic.common.constant.ChatbotConstants;
import vn.edu.utc.comic.genre.dto.GenreTagResponse;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.story.dto.StoryDetailResponse;
import vn.edu.utc.comic.story.entity.Story;

/** Ánh xạ cho chatbot. Ánh xạ từ Story phải gọi trong transaction vì đọc thể loại (quan hệ lazy). */
@Mapper
public interface ChatMapper {

    String GENRE_SLUGS = "genreSlugs";
    String TAG_SLUGS = "tagSlugs";
    String SHORT_DESCRIPTION = "shortDescription";
    String DETAIL_DESCRIPTION = "detailDescription";
    String RATING_AVERAGE = "ratingAverage";
    char ELLIPSIS = '…';

    @Mapping(target = "genres", source = "genres", qualifiedByName = GENRE_SLUGS)
    @Mapping(target = "shortDescription", source = "description", qualifiedByName = SHORT_DESCRIPTION)
    @Mapping(target = "ratingAverage", source = "story", qualifiedByName = RATING_AVERAGE)
    StoryToolItem toToolItem(Story story);

    List<StoryToolItem> toToolItems(List<Story> stories);

    @Mapping(target = "genres", source = "genres", qualifiedByName = TAG_SLUGS)
    @Mapping(target = "description", source = "description", qualifiedByName = DETAIL_DESCRIPTION)
    StoryToolDetail toToolDetail(StoryDetailResponse story);

    ChatConversationResponse toResponse(ChatConversation conversation);

    List<ChatConversationResponse> toResponses(List<ChatConversation> conversations);

    @Mapping(target = "source", source = "message.answerSource")
    @Mapping(target = "id", source = "message.id")
    @Mapping(target = "createdAt", source = "message.createdAt")
    ChatMessageResponse toResponse(ChatMessage message, List<ChatCardResponse> cards);

    ChatHistoryEntry toHistoryEntry(ChatMessage message);

    @Named(GENRE_SLUGS)
    default List<String> toGenreSlugs(Set<Genre> genres) {
        return genres.stream().map(Genre::getSlug).toList();
    }

    @Named(TAG_SLUGS)
    default List<String> toTagSlugs(List<GenreTagResponse> genres) {
        return genres.stream().map(GenreTagResponse::slug).toList();
    }

    @Named(SHORT_DESCRIPTION)
    default String toShortDescription(String description) {
        return truncate(description, ChatbotConstants.TOOL_SHORT_DESCRIPTION_LENGTH);
    }

    @Named(DETAIL_DESCRIPTION)
    default String toDetailDescription(String description) {
        return truncate(description, ChatbotConstants.TOOL_DETAIL_DESCRIPTION_LENGTH);
    }

    @Named(RATING_AVERAGE)
    default Double toRatingAverage(Story story) {
        return story.getRatingCount() == 0 ? null : (double) story.getRatingSum() / story.getRatingCount();
    }

    /** Cắt mô tả để kết quả hàm gọn: mô hình chỉ cần đủ ý để giải thích lý do gợi ý. */
    static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + ELLIPSIS;
    }
}
