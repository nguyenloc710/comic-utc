package vn.edu.utc.comic.chatbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.comic.chatbot.dto.ChatConversationResponse;
import vn.edu.utc.comic.chatbot.dto.ChatFeedbackRequest;
import vn.edu.utc.comic.chatbot.dto.ChatMessageRequest;
import vn.edu.utc.comic.chatbot.dto.ChatMessageResponse;
import vn.edu.utc.comic.chatbot.dto.ChatReplyResponse;
import vn.edu.utc.comic.chatbot.service.ChatHistoryService;
import vn.edu.utc.comic.chatbot.service.ChatbotService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.ApiResponse;
import vn.edu.utc.comic.common.security.AppUserPrincipal;

/**
 * API của khung chat gợi ý truyện. Chỉ người đã đăng nhập; mọi thao tác lấy người dùng từ phiên, hội thoại của
 * người khác được coi như không tồn tại.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_CHAT_PATH)
@Tag(name = "Chatbot gợi ý truyện", description = "Gửi tin nhắn, xem lại hội thoại, đánh giá câu trả lời")
public class ChatApiController {

    private final ChatbotService chatbotService;
    private final ChatHistoryService chatHistoryService;

    @PostMapping("/messages")
    @Operation(summary = "Gửi tin nhắn cho chatbot",
            description = "Bỏ trống conversationId để bắt đầu hội thoại mới. source = FALLBACK_KEYWORD nghĩa là mô hình "
                    + "đang lỗi và kết quả là tìm theo từ khóa. Lỗi: 400 CHAT_MESSAGE_TOO_LONG, 404 "
                    + "CHAT_CONVERSATION_NOT_FOUND, 429 CHAT_DAILY_LIMIT, 503 CHAT_DISABLED.")
    public ApiResponse<ChatReplyResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request,
                                                      @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(chatbotService.reply(principal.getId(), request));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Các hội thoại gần nhất của tôi")
    public ApiResponse<List<ChatConversationResponse>> listConversations(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(chatHistoryService.findConversations(principal.getId()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Tin nhắn của một hội thoại kèm thẻ truyện",
            description = "Lỗi: 404 CHAT_CONVERSATION_NOT_FOUND nếu không có hoặc của người khác.")
    public ApiResponse<List<ChatMessageResponse>> listMessages(@PathVariable Long conversationId,
                                                               @AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(chatHistoryService.findMessages(conversationId, principal.getId()));
    }

    @PostMapping("/messages/{messageId}/feedback")
    @Operation(summary = "Đánh giá câu trả lời",
            description = "value: 1 = hữu ích, -1 = không hữu ích, 0 = bỏ đánh giá. Lỗi: 404 CHAT_MESSAGE_NOT_FOUND.")
    public ApiResponse<Void> recordFeedback(@PathVariable Long messageId, @Valid @RequestBody ChatFeedbackRequest request,
                                            @AuthenticationPrincipal AppUserPrincipal principal) {
        chatHistoryService.recordFeedback(messageId, principal.getId(), request.value());
        return ApiResponse.success(null);
    }
}
