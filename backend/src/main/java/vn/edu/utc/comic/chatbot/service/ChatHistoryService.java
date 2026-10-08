package vn.edu.utc.comic.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chatbot.dto.AssistantDraft;
import vn.edu.utc.comic.chatbot.dto.ChatCardResponse;
import vn.edu.utc.comic.chatbot.dto.ChatConversationResponse;
import vn.edu.utc.comic.chatbot.dto.ChatHistoryEntry;
import vn.edu.utc.comic.chatbot.dto.ChatMessageRequest;
import vn.edu.utc.comic.chatbot.dto.ChatMessageResponse;
import vn.edu.utc.comic.chatbot.dto.ChatReplyResponse;
import vn.edu.utc.comic.chatbot.dto.ChatTurn;
import vn.edu.utc.comic.chatbot.dto.StoredRecommendation;
import vn.edu.utc.comic.chatbot.dto.ToolTraceEntry;
import vn.edu.utc.comic.chatbot.entity.ChatConversation;
import vn.edu.utc.comic.chatbot.entity.ChatMessage;
import vn.edu.utc.comic.chatbot.enums.ChatRole;
import vn.edu.utc.comic.chatbot.mapper.ChatMapper;
import vn.edu.utc.comic.chatbot.repository.ChatConversationRepository;
import vn.edu.utc.comic.chatbot.repository.ChatMessageRepository;
import vn.edu.utc.comic.common.constant.ChatbotConstants;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.story.dto.StoryCardResponse;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Lưu và đọc hội thoại chatbot.
 *
 * <p>Một lượt hỏi chia làm HAI transaction ngắn — nhận câu hỏi ({@link #startTurn}) và lưu câu trả lời
 * ({@link #finishTurn}) — để không giữ kết nối cơ sở dữ liệu trong cả chục giây chờ nhà cung cấp mô hình.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final StoryCatalogQueryService storyCatalogQueryService;
    private final SettingService settingService;
    private final ChatMapper chatMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Nhận một câu hỏi: kiểm tra bật/tắt, độ dài, hạn mức ngày; lưu tin của người dùng; nạp lịch sử để gửi mô hình.
     *
     * @throws ApiException CHAT_DISABLED, CHAT_MESSAGE_TOO_LONG, CHAT_DAILY_LIMIT,
     *                      CHAT_CONVERSATION_NOT_FOUND nếu hội thoại không tồn tại hoặc của người khác
     */
    @Transactional
    public ChatTurn startTurn(Long userId, ChatMessageRequest request) {
        // Khóa dòng tài khoản bằng câu lệnh ĐẦU TIÊN: hai tab gửi cùng lúc phải đếm hạn mức lần lượt, không cùng
        // thấy "còn 1 lượt" rồi cùng vượt
        userAccountRepository.lockForUpdate(userId);
        String content = request.content().trim();
        validateMaySend(userId, content);
        Instant now = clock.instant();
        ChatConversation conversation = request.conversationId() == null
                ? createConversation(userId, content, now)
                : getOwnedConversation(request.conversationId(), userId);
        List<ChatHistoryEntry> history = loadHistory(conversation.getId());
        conversation.setLastMessageAt(now);
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(ChatRole.USER);
        message.setContent(content);
        messageRepository.save(message);
        return new ChatTurn(userId, conversation.getId(), content, history, collectStoryIds(history));
    }

    /** Lưu câu trả lời của trợ lý và trả về dữ liệu cho khung chat. */
    @Transactional
    public ChatReplyResponse finishTurn(ChatTurn turn, AssistantDraft draft, int latencyMs) {
        ChatConversation conversation = conversationRepository.getReferenceById(turn.conversationId());
        conversation.setLastMessageAt(clock.instant());
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(ChatRole.ASSISTANT);
        message.setContent(draft.reply());
        message.setRecommendations(toJson(draft.cards().stream().map(ChatHistoryService::toStored).toList()));
        message.setToolTrace(draft.toolTrace().isEmpty() ? null : toJson(draft.toolTrace()));
        message.setAnswerSource(draft.source());
        message.setFallbackReason(draft.fallbackReason());
        message.setRejectedCount(draft.rejectedCount());
        message.setPromptTokens(draft.promptTokens());
        message.setCompletionTokens(draft.completionTokens());
        message.setLatencyMs(latencyMs);
        messageRepository.save(message);
        return new ChatReplyResponse(turn.conversationId(), message.getId(), draft.reply(), draft.cards(), draft.source());
    }

    /** Các hội thoại gần nhất của một người. */
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> findConversations(Long userId) {
        return chatMapper.toResponses(conversationRepository.findByUserIdOrderByLastMessageAtDescIdDesc(userId,
                PageRequest.of(0, ChatbotConstants.CONVERSATION_LIST_SIZE)));
    }

    /**
     * Tin nhắn của một hội thoại, cũ nhất trước, kèm thẻ truyện dựng lại từ cơ sở dữ liệu (truyện không còn công
     * khai thì không hiện).
     *
     * @throws ApiException CHAT_CONVERSATION_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> findMessages(Long conversationId, Long userId) {
        getOwnedConversation(conversationId, userId);
        List<ChatMessage> messages = new ArrayList<>(messageRepository.findByConversationIdOrderByIdDesc(conversationId,
                PageRequest.of(0, ChatbotConstants.CONVERSATION_MESSAGE_LIMIT)));
        Collections.reverse(messages);
        Map<Long, StoryCardResponse> cardsById = loadCards(messages);
        return messages.stream()
                .map(message -> chatMapper.toResponse(message, rebuildCards(message, cardsById)))
                .toList();
    }

    /**
     * Ghi đánh giá cho một câu trả lời của trợ lý; 0 để bỏ đánh giá.
     *
     * @throws ApiException CHAT_MESSAGE_NOT_FOUND nếu không có, của người khác, hoặc không phải câu trả lời
     */
    @Transactional
    public void recordFeedback(Long messageId, Long userId, int value) {
        ChatMessage message = messageRepository.findOwned(messageId, userId)
                .filter(found -> found.getRole() == ChatRole.ASSISTANT)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
        message.setFeedback(value == 0 ? null : value);
    }

    private void validateMaySend(Long userId, String content) {
        if (!settingService.getBoolean(SettingKeys.CHAT_ENABLED, ChatbotConstants.DEFAULT_ENABLED)) {
            throw new ApiException(ErrorCode.CHAT_DISABLED);
        }
        int maxLength = settingService.getInt(SettingKeys.CHAT_MAX_MESSAGE_LENGTH, ChatbotConstants.DEFAULT_MAX_MESSAGE_LENGTH);
        if (content.length() > maxLength) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_TOO_LONG, maxLength);
        }
        int dailyLimit = settingService.getInt(SettingKeys.CHAT_DAILY_LIMIT_PER_USER, ChatbotConstants.DEFAULT_DAILY_LIMIT);
        // "Hôm nay" tính theo giờ Việt Nam: hạn mức được làm mới lúc nửa đêm của người dùng
        Instant startOfDay = LocalDate.now(clock.withZone(DateTimeConstants.DISPLAY_ZONE))
                .atStartOfDay(DateTimeConstants.DISPLAY_ZONE).toInstant();
        if (messageRepository.countByUserSince(userId, ChatRole.USER, startOfDay) >= dailyLimit) {
            throw new ApiException(ErrorCode.CHAT_DAILY_LIMIT, dailyLimit);
        }
    }

    private ChatConversation createConversation(Long userId, String content, Instant now) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setTitle(content.length() <= ChatbotConstants.CONVERSATION_TITLE_LENGTH
                ? content
                : content.substring(0, ChatbotConstants.CONVERSATION_TITLE_LENGTH));
        conversation.setLastMessageAt(now);
        return conversationRepository.save(conversation);
    }

    private ChatConversation getOwnedConversation(Long conversationId, Long userId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_CONVERSATION_NOT_FOUND));
    }

    /** Cửa sổ lịch sử gửi kèm cho mô hình, cũ nhất trước. */
    private List<ChatHistoryEntry> loadHistory(Long conversationId) {
        int window = settingService.getInt(SettingKeys.CHAT_HISTORY_WINDOW, ChatbotConstants.DEFAULT_HISTORY_WINDOW);
        List<ChatHistoryEntry> history = new ArrayList<>(messageRepository
                .findByConversationIdOrderByIdDesc(conversationId, PageRequest.of(0, Math.max(window, 1)))
                .stream().map(chatMapper::toHistoryEntry).toList());
        Collections.reverse(history);
        return history;
    }

    /** Id truyện đã đi ra từ kết quả hàm (và đã được gợi ý) ở các lượt trước trong cửa sổ lịch sử. */
    private Set<Long> collectStoryIds(List<ChatHistoryEntry> history) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ChatHistoryEntry entry : history) {
            readList(entry.toolTrace(), new TypeReference<List<ToolTraceEntry>>() { })
                    .forEach(call -> ids.addAll(call.storyIds()));
            readList(entry.recommendations(), new TypeReference<List<StoredRecommendation>>() { })
                    .forEach(story -> ids.add(story.storyId()));
        }
        return ids;
    }

    /** Một truy vấn cho mọi thẻ truyện của cả hội thoại, thay vì một truy vấn mỗi tin nhắn. */
    private Map<Long, StoryCardResponse> loadCards(List<ChatMessage> messages) {
        List<Long> ids = messages.stream()
                .flatMap(message -> readList(message.getRecommendations(),
                        new TypeReference<List<StoredRecommendation>>() { }).stream())
                .map(StoredRecommendation::storyId)
                .distinct()
                .toList();
        return storyCatalogQueryService.findCardsByIds(ids).stream()
                .collect(Collectors.toMap(StoryCardResponse::id, Function.identity()));
    }

    private List<ChatCardResponse> rebuildCards(ChatMessage message, Map<Long, StoryCardResponse> cardsById) {
        return readList(message.getRecommendations(), new TypeReference<List<StoredRecommendation>>() { }).stream()
                .filter(stored -> cardsById.containsKey(stored.storyId()))
                .map(stored -> new ChatCardResponse(cardsById.get(stored.storyId()), stored.reason()))
                .toList();
    }

    private static StoredRecommendation toStored(ChatCardResponse card) {
        return new StoredRecommendation(card.story().id(), card.story().title(), card.story().chapterCount(),
                card.reason());
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            log.warn("Bỏ qua dữ liệu chatbot không đọc được", exception);
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không chuyển được dữ liệu chatbot sang JSON", exception);
        }
    }
}
