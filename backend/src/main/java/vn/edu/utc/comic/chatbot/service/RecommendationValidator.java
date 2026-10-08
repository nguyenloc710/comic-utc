package vn.edu.utc.comic.chatbot.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.utc.comic.chatbot.dto.ChatCardResponse;
import vn.edu.utc.comic.chatbot.dto.Recommendation;
import vn.edu.utc.comic.chatbot.dto.ValidatedRecommendations;
import vn.edu.utc.comic.common.constant.ChatbotConstants;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Hậu kiểm gợi ý của mô hình trước khi hiển thị (docs/04 §5). Đây là hàng rào chống "bịa": dù mô hình viết gì,
 * thẻ truyện người dùng thấy chỉ gồm truyện có thật, đang công khai và đã thật sự đi ra từ kết quả hàm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationValidator {

    private final StoryCatalogQueryService storyCatalogQueryService;
    private final SettingService settingService;

    /**
     * @param recommendations gợi ý mô hình trả về, theo thứ tự mô hình xếp
     * @param allowedStoryIds id truyện các hàm đã trả ở lượt này và các lượt trước trong cửa sổ lịch sử
     * @return thẻ truyện dựng hoàn toàn từ cơ sở dữ liệu (từ mô hình chỉ lấy lý do) và số gợi ý bị loại
     */
    public ValidatedRecommendations validate(List<Recommendation> recommendations, Set<Long> allowedStoryIds) {
        // 1. Chỉ giữ id có trong kết quả hàm, bỏ trùng (giữ lý do của lần xuất hiện đầu)
        Map<Long, String> reasons = new LinkedHashMap<>();
        for (Recommendation recommendation : recommendations) {
            if (recommendation.storyId() != null && allowedStoryIds.contains(recommendation.storyId())) {
                reasons.putIfAbsent(recommendation.storyId(), blankToNull(recommendation.reason()));
            }
        }
        // 2. Nạp lại từ cơ sở dữ liệu với điều kiện công khai: truyện vừa bị ẩn giữa chừng cũng rơi
        int maxCards = settingService.getInt(SettingKeys.CHAT_MAX_RECOMMENDATIONS,
                ChatbotConstants.DEFAULT_MAX_RECOMMENDATIONS);
        List<StoryCardResponse> stories = storyCatalogQueryService.findCardsByIds(List.copyOf(reasons.keySet()));
        List<ChatCardResponse> cards = new ArrayList<>();
        for (StoryCardResponse story : stories) {
            if (cards.size() < maxCards) {
                cards.add(new ChatCardResponse(story, reasons.get(story.id())));
            }
        }
        int uniqueProposals = (int) recommendations.stream().map(Recommendation::storyId).filter(Objects::nonNull)
                .distinct().count();
        int rejected = Math.max(0, uniqueProposals - stories.size());
        if (rejected > 0) {
            log.info("Hậu kiểm loại {} gợi ý của chatbot", rejected);
        }
        return new ValidatedRecommendations(cards, rejected);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
