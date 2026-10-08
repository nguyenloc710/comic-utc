package vn.edu.utc.comic.chatbot.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.chatbot.dto.AssistantDraft;
import vn.edu.utc.comic.chatbot.dto.ChatCardResponse;
import vn.edu.utc.comic.chatbot.enums.AnswerSource;
import vn.edu.utc.comic.chatbot.enums.FallbackReason;
import vn.edu.utc.comic.common.constant.ChatbotConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.i18n.MessageService;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;

/**
 * Đường lui khi mô hình lỗi, quá thời gian chờ hoặc trả lời hỏng: lọc theo thể loại / loại truyện nhận ra trong
 * câu, hoặc tìm toàn văn theo nguyên câu, rồi trả thẳng thẻ truyện kèm một câu giải thích.
 * Người dùng vẫn nhận được kết quả có ích thay vì một thông báo lỗi.
 */
@Component
@RequiredArgsConstructor
public class KeywordFallbackResponder {

    private final ChatbotCatalogService chatbotCatalogService;
    private final MessageService messageService;
    private final SettingService settingService;

    public AssistantDraft respond(String content, FallbackReason reason) {
        int limit = settingService.getInt(SettingKeys.CHAT_MAX_RECOMMENDATIONS, ChatbotConstants.DEFAULT_MAX_RECOMMENDATIONS);
        List<ChatCardResponse> cards = chatbotCatalogService.searchForFallback(content, limit).stream()
                .map(story -> new ChatCardResponse(story, null))
                .toList();
        String reply = messageService.getMessage(cards.isEmpty()
                ? MessageKeys.CHAT_FALLBACK_EMPTY
                : MessageKeys.CHAT_FALLBACK_FOUND);
        return new AssistantDraft(AnswerSource.FALLBACK_KEYWORD, reason, reply, cards, List.of(), 0, null, null);
    }
}
