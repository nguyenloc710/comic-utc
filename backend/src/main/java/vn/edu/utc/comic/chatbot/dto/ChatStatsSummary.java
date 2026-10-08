package vn.edu.utc.comic.chatbot.dto;

/**
 * Tổng hợp số liệu chatbot trong một khoảng thời gian. Tham số kiểu bao vì SUM / AVG trên tập rỗng trả NULL;
 * constructor đổi NULL thành 0.
 */
public record ChatStatsSummary(
        Long userMessages,
        Long conversations,
        Long users,
        Long llmAnswers,
        Long fallbackAnswers,
        Double averageLatencyMs,
        Double averagePromptTokens,
        Double averageCompletionTokens,
        Long rejectedRecommendations,
        Long positiveFeedback,
        Long negativeFeedback) {

    public ChatStatsSummary {
        userMessages = orZero(userMessages);
        conversations = orZero(conversations);
        users = orZero(users);
        llmAnswers = orZero(llmAnswers);
        fallbackAnswers = orZero(fallbackAnswers);
        rejectedRecommendations = orZero(rejectedRecommendations);
        positiveFeedback = orZero(positiveFeedback);
        negativeFeedback = orZero(negativeFeedback);
    }

    /** Tỉ lệ câu trả lời phải rơi về đường lui, tính theo phần trăm; {@code null} khi chưa có câu trả lời nào. */
    public Double fallbackRatePercent() {
        long answers = llmAnswers + fallbackAnswers;
        return answers == 0 ? null : fallbackAnswers * 100.0 / answers;
    }

    private static Long orZero(Long value) {
        return value == null ? Long.valueOf(0) : value;
    }
}
