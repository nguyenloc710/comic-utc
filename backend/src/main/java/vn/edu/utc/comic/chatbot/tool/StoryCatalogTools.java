package vn.edu.utc.comic.chatbot.tool;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.chatbot.dto.StoryDetailToolResult;
import vn.edu.utc.comic.chatbot.dto.StoryListToolResult;
import vn.edu.utc.comic.chatbot.dto.StorySearchToolRequest;
import vn.edu.utc.comic.chatbot.dto.StorySearchToolResult;
import vn.edu.utc.comic.chatbot.enums.TrendingPeriod;
import vn.edu.utc.comic.chatbot.service.ChatbotCatalogService;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Các hàm cho mô hình tra cứu kho truyện.
 *
 * <p>Tất cả đều CHỈ ĐỌC và đi qua {@link ChatbotCatalogService} (vốn chỉ dùng StoryCatalogQueryService), nên mô
 * hình bị dụ thế nào cũng chỉ thấy truyện đang công khai. Mỗi hàm ghi id truyện đã trả vào
 * {@link ToolResultCollector} — nguồn của bước hậu kiểm gợi ý.
 */
@Component
@RequiredArgsConstructor
public class StoryCatalogTools {

    private final ChatbotCatalogService chatbotCatalogService;

    @Tool(name = ChatToolDescriptions.SEARCH_STORIES_NAME, description = ChatToolDescriptions.SEARCH_STORIES)
    public StorySearchToolResult searchStories(StorySearchToolRequest request, ToolContext toolContext) {
        StorySearchToolResult result = chatbotCatalogService.searchStories(request);
        ToolResultCollector.from(toolContext).record(ChatToolDescriptions.SEARCH_STORIES_NAME, request, result.storyIds());
        return result;
    }

    @Tool(name = ChatToolDescriptions.GET_STORY_DETAIL_NAME, description = ChatToolDescriptions.GET_STORY_DETAIL)
    public StoryDetailToolResult getStoryDetail(
            @ToolParam(description = ChatToolDescriptions.STORY_ID_PARAM) Long storyId, ToolContext toolContext) {
        StoryDetailToolResult result = chatbotCatalogService.getStoryDetail(storyId);
        ToolResultCollector.from(toolContext).record(ChatToolDescriptions.GET_STORY_DETAIL_NAME, storyId,
                result.found() ? List.of(result.story().id()) : List.of());
        return result;
    }

    @Tool(name = ChatToolDescriptions.FIND_SIMILAR_STORIES_NAME, description = ChatToolDescriptions.FIND_SIMILAR_STORIES)
    public StoryListToolResult findSimilarStories(
            @ToolParam(description = ChatToolDescriptions.STORY_ID_PARAM) Long storyId,
            @ToolParam(required = false, description = ChatToolDescriptions.LIMIT_PARAM) Integer limit,
            ToolContext toolContext) {
        StoryListToolResult result = chatbotCatalogService.findSimilarStories(storyId, limit);
        ToolResultCollector.from(toolContext).record(ChatToolDescriptions.FIND_SIMILAR_STORIES_NAME, storyId,
                result.storyIds());
        return result;
    }

    @Tool(name = ChatToolDescriptions.GET_TRENDING_STORIES_NAME, description = ChatToolDescriptions.GET_TRENDING_STORIES)
    public StoryListToolResult getTrendingStories(
            @ToolParam(description = ChatToolDescriptions.PERIOD_PARAM) TrendingPeriod period,
            @ToolParam(required = false, description = ChatToolDescriptions.TYPE_PARAM) StoryType type,
            @ToolParam(required = false, description = ChatToolDescriptions.LIMIT_PARAM) Integer limit,
            ToolContext toolContext) {
        StoryListToolResult result = chatbotCatalogService.findTrendingStories(period, type, limit);
        ToolResultCollector.from(toolContext).record(ChatToolDescriptions.GET_TRENDING_STORIES_NAME, period,
                result.storyIds());
        return result;
    }
}
