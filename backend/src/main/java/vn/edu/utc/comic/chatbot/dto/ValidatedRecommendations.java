package vn.edu.utc.comic.chatbot.dto;

import java.util.List;

/** Kết quả hậu kiểm gợi ý: thẻ được giữ lại và số gợi ý bị loại. */
public record ValidatedRecommendations(List<ChatCardResponse> cards, int rejectedCount) {
}
