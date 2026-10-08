package vn.edu.utc.comic.chatbot.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chatbot.dto.AdminChatStatsResponse;
import vn.edu.utc.comic.chatbot.dto.ChatNegativeFeedbackResponse;
import vn.edu.utc.comic.chatbot.repository.ChatMessageRepository;
import vn.edu.utc.comic.common.constant.ChatbotConstants;

/**
 * Số liệu chatbot cho quản trị viên: lượng dùng, tỉ lệ rơi về đường lui, độ trễ, token, số gợi ý bị hậu kiểm
 * loại và phản hồi của người dùng — đủ để biết chatbot có đang chạy tốt và sửa prompt có tốt lên thật không.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatStatsService {

    private final ChatMessageRepository messageRepository;
    private final Clock clock;

    /** Số liệu {@link ChatbotConstants#ADMIN_STATS_DAYS} ngày gần nhất và các câu trả lời bị chê gần nhất. */
    public AdminChatStatsResponse getStats() {
        Instant from = clock.instant().minus(Duration.ofDays(ChatbotConstants.ADMIN_STATS_DAYS));
        return new AdminChatStatsResponse(
                ChatbotConstants.ADMIN_STATS_DAYS,
                messageRepository.summarizeSince(from),
                messageRepository.countFallbackReasonsSince(from),
                messageRepository.findRecentNegative(PageRequest.of(0, ChatbotConstants.ADMIN_RECENT_NEGATIVE_SIZE))
                        .stream()
                        .map(message -> new ChatNegativeFeedbackResponse(message.getId(),
                                message.getConversation().getTitle(), message.getContent(), message.getCreatedAt()))
                        .toList());
    }
}
