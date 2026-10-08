package vn.edu.utc.comic.chatbot.dto;

import java.util.List;

/** Dữ liệu trang quản trị chatbot. */
public record AdminChatStatsResponse(
        int days,
        ChatStatsSummary summary,
        List<ChatReasonCount> fallbackReasons,
        List<ChatNegativeFeedbackResponse> recentNegative) {
}
