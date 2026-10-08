package vn.edu.utc.comic.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.support.ScriptedChatModel;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Luồng chat đầy đủ qua API với mô hình giả có kịch bản: hàm tra cứu thật, hậu kiểm thật, lưu lịch sử thật.
 */
class ChatbotIntegrationTest extends AbstractIntegrationTest {

    private static final String LIMIT_KEY = "chat.daily_limit_per_user";

    @Autowired
    private ScriptedChatModel chatModel;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SettingService settingService;

    @BeforeEach
    @AfterEach
    void resetModel() {
        chatModel.reset();
    }

    @Test
    void reply_showsOnlyStoriesThatToolsReturned_builtFromTheDatabase() throws Exception {
        String marker = "chat" + uniqueSuffix();
        Story found = createStory(StoryType.NOVEL, story -> story.setTitle("Tu Tiên " + marker));
        Story hidden = createStory(StoryType.NOVEL, story -> {
            story.setTitle("Ẩn " + marker);
            story.setVisibility(StoryVisibility.HIDDEN);
        });
        Story neverReturned = createPublishedStory(StoryType.NOVEL);
        chatModel.respond(prompt -> {
            ScriptedChatModel.callTool(prompt, "searchStories", searchArgs(marker));
            // Mô hình "bịa": gợi ý thêm một truyện hàm không trả, một truyện bị ẩn và một id không tồn tại
            return answer("Đây là truyện hợp với bạn", found.getId(), neverReturned.getId(), hidden.getId(), 999_999_999L);
        });
        UserAccount reader = createAccount(Role.USER);

        JsonNode reply = send(reader, null, "truyện tu tiên").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString().transform(this::data);

        assertThat(reply.path("reply").asText()).isEqualTo("Đây là truyện hợp với bạn");
        assertThat(reply.path("source").asText()).isEqualTo("LLM");
        assertThat(reply.path("cards")).hasSize(1);
        JsonNode card = reply.path("cards").get(0);
        // Tên, slug lấy từ cơ sở dữ liệu; từ mô hình chỉ lấy lý do
        assertThat(card.path("story").path("title").asText()).isEqualTo(found.getTitle());
        assertThat(card.path("story").path("slug").asText()).isEqualTo(found.getSlug());
        assertThat(card.path("reason").asText()).isEqualTo("Lý do " + found.getId());
        Map<String, Object> saved = assistantRow(reply.path("messageId").asLong());
        assertThat(saved).containsEntry("answer_source", "LLM").containsEntry("rejected_count", 3);
        assertThat((String) saved.get("tool_trace")).contains("searchStories").contains(String.valueOf(found.getId()));
    }

    @Test
    void systemPrompt_carriesGenresAndOutputFormat_andToolsAreRegistered() throws Exception {
        send(createAccount(Role.USER), null, "xin chào").andExpect(status().isOk());

        Prompt prompt = chatModel.prompts().getLast();
        String system = prompt.getInstructions().getFirst().getText();
        assertThat(prompt.getInstructions().getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(system).contains("tu-tien — Tu tiên").contains("\"recommendations\"").doesNotContain("{genres}");
        assertThat(prompt.getInstructions().getLast().getText()).isEqualTo("xin chào");
        assertThat(((org.springframework.ai.model.tool.ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder("searchStories", "getStoryDetail", "findSimilarStories", "getTrendingStories");
    }

    @Test
    void followUpQuestion_seesHistoryWithASummary_andMayReuseStoriesFromEarlierTurns() throws Exception {
        String marker = "noitiep" + uniqueSuffix();
        Story story = createStory(StoryType.NOVEL, created -> {
            created.setTitle("Hỏi Nối Tiếp " + marker);
            created.setChapterCount(180);
        });
        UserAccount reader = createAccount(Role.USER);
        chatModel.respond(prompt -> {
            ScriptedChatModel.callTool(prompt, "searchStories", searchArgs(marker));
            return answer("Lượt một", story.getId());
        });
        long conversationId = data(send(reader, null, "lượt một").andReturn().getResponse().getContentAsString())
                .path("conversationId").asLong();

        // Lượt hai không gọi hàm mà nhắc lại truyện của lượt trước: vẫn hợp lệ
        chatModel.respond(prompt -> answer("Lượt hai", story.getId()));
        JsonNode second = data(send(reader, conversationId, "cái đó nói về gì").andReturn().getResponse()
                .getContentAsString());

        assertThat(second.path("cards")).hasSize(1);
        List<Message> sent = chatModel.prompts().getLast().getInstructions();
        assertThat(sent).extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
        assertThat(sent.get(2).getText()).contains("Lượt một").contains("#" + story.getId())
                .contains("(180 chương)").contains("searchStories");
    }

    @Test
    void plainTextAnswer_isAcceptedWithTheLastToolResults() throws Exception {
        String marker = "vanban" + uniqueSuffix();
        Story story = createStory(StoryType.COMIC, created -> created.setTitle("Văn Bản " + marker));
        chatModel.respond(prompt -> {
            ScriptedChatModel.callTool(prompt, "searchStories", searchArgs(marker));
            return "Mình tìm được một truyện tranh cho bạn.";
        });

        JsonNode reply = data(send(createAccount(Role.USER), null, "truyện tranh").andReturn().getResponse()
                .getContentAsString());

        assertThat(reply.path("source").asText()).isEqualTo("LLM");
        assertThat(reply.path("reply").asText()).isEqualTo("Mình tìm được một truyện tranh cho bạn.");
        assertThat(reply.path("cards").get(0).path("story").path("id").asLong()).isEqualTo(story.getId());
    }

    @Test
    void modelFailures_fallBackToKeywordSearch_andRecordTheReason() throws Exception {
        String marker = "duonglui" + uniqueSuffix();
        Story story = createStory(StoryType.NOVEL, created -> created.setTitle("Đường Lui " + marker));
        UserAccount reader = createAccount(Role.USER);
        Map<String, java.util.function.Function<Prompt, String>> failures = new LinkedHashMap<>();
        failures.put("TIMEOUT", prompt -> {
            throw new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out"));
        });
        failures.put("PROVIDER_ERROR", prompt -> {
            throw new IllegalStateException("401 invalid x-api-key");
        });
        failures.put("INVALID_RESPONSE", prompt -> "{\"reply\": \"cắt giữa chừng");

        for (Map.Entry<String, java.util.function.Function<Prompt, String>> failure : failures.entrySet()) {
            chatModel.respond(failure.getValue());
            JsonNode reply = data(send(reader, null, marker).andExpect(status().isOk()).andReturn().getResponse()
                    .getContentAsString());

            assertThat(reply.path("source").asText()).isEqualTo("FALLBACK_KEYWORD");
            assertThat(reply.path("reply").asText()).contains("từ khóa");
            assertThat(reply.path("cards").get(0).path("story").path("id").asLong()).isEqualTo(story.getId());
            assertThat(reply.path("cards").get(0).path("reason").isNull()).isTrue();
            assertThat(assistantRow(reply.path("messageId").asLong())).containsEntry("fallback_reason", failure.getKey());
        }
    }

    @Test
    void conversationsAreOwnedByTheirUser() throws Exception {
        UserAccount owner = createAccount(Role.USER);
        UserAccount stranger = createAccount(Role.USER);
        long conversationId = data(send(owner, null, "của tôi").andReturn().getResponse().getContentAsString())
                .path("conversationId").asLong();
        long messageId = data(mockMvc.perform(get("/api/chat/conversations/{id}/messages", conversationId)
                        .with(user(principalOf(owner)))).andReturn().getResponse().getContentAsString())
                .get(1).path("id").asLong();

        send(stranger, conversationId, "chen vào").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_CONVERSATION_NOT_FOUND"));
        mockMvc.perform(get("/api/chat/conversations/{id}/messages", conversationId).with(user(principalOf(stranger))))
                .andExpect(status().isNotFound());
        feedback(stranger, messageId, 1).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/chat/conversations").with(user(principalOf(stranger))))
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/chat/conversations").with(user(principalOf(owner))))
                .andExpect(jsonPath("$.data[0].id").value(conversationId))
                .andExpect(jsonPath("$.data[0].title").value("của tôi"));
    }

    @Test
    void history_isReloadedWithCards_andFeedbackCanBeGivenAndCleared() throws Exception {
        String marker = "lichsu" + uniqueSuffix();
        Story story = createStory(StoryType.NOVEL, created -> created.setTitle("Lịch Sử " + marker));
        UserAccount reader = createAccount(Role.USER);
        chatModel.respond(prompt -> {
            ScriptedChatModel.callTool(prompt, "searchStories", searchArgs(marker));
            return answer("Gợi ý", story.getId());
        });
        JsonNode reply = data(send(reader, null, "gợi ý đi").andReturn().getResponse().getContentAsString());
        long messageId = reply.path("messageId").asLong();

        feedback(reader, messageId, -1).andExpect(status().isOk());
        mockMvc.perform(get("/api/chat/conversations/{id}/messages", reply.path("conversationId").asLong())
                        .with(user(principalOf(reader))))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[1].cards[0].story.id").value(story.getId()))
                .andExpect(jsonPath("$.data[1].cards[0].reason").value("Lý do " + story.getId()))
                .andExpect(jsonPath("$.data[1].feedback").value(-1));
        feedback(reader, messageId, 0).andExpect(status().isOk());
        assertThat(assistantRow(messageId).get("feedback")).isNull();
        feedback(reader, messageId, 5).andExpect(status().isBadRequest());

        // Truyện bị ẩn sau đó thì không còn hiện khi nạp lại hội thoại
        jdbcTemplate.update("UPDATE story SET visibility = 'HIDDEN' WHERE id = ?", story.getId());
        mockMvc.perform(get("/api/chat/conversations/{id}/messages", reply.path("conversationId").asLong())
                        .with(user(principalOf(reader))))
                .andExpect(jsonPath("$.data[1].cards.length()").value(0));
    }

    @Test
    void guards_rejectGuestsLongMessagesDisabledChatAndDailyLimit_beforeCallingTheModel() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        String originalLimit = setting(LIMIT_KEY);
        try {
            mockMvc.perform(post("/api/chat/messages").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\": \"khách\"}")).andExpect(status().isUnauthorized());
            send(reader, null, "x".repeat(501)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("CHAT_MESSAGE_TOO_LONG"));
            send(reader, null, "   ").andExpect(status().isBadRequest());

            setSetting("chat.enabled", "false");
            send(reader, null, "đang tắt").andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.errorCode").value("CHAT_DISABLED"));
            setSetting("chat.enabled", "true");

            setSetting(LIMIT_KEY, "2");
            send(reader, null, "một").andExpect(status().isOk());
            send(reader, null, "hai").andExpect(status().isOk());
            send(reader, null, "ba").andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.errorCode").value("CHAT_DAILY_LIMIT"));
        } finally {
            setSetting(LIMIT_KEY, originalLimit);
            setSetting("chat.enabled", "true");
        }
        // Chỉ hai lượt được chấp nhận mới tới mô hình
        assertThat(chatModel.prompts()).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM chat_message m JOIN chat_conversation c ON c.id = m.conversation_id
                WHERE c.user_id = ? AND m.role = 'USER'
                """, Long.class, reader.getId())).isEqualTo(2);
    }

    private ResultActions send(UserAccount account, Long conversationId, String content) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conversationId", conversationId);
        body.put("content", content);
        return mockMvc.perform(post("/api/chat/messages").with(csrf()).with(user(principalOf(account)))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions feedback(UserAccount account, long messageId, int value) throws Exception {
        return mockMvc.perform(post("/api/chat/messages/{id}/feedback", messageId).with(csrf())
                .with(user(principalOf(account))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\": " + value + "}"));
    }

    /** JSON câu trả lời của mô hình giả: lời đáp và gợi ý các id cho trước, lý do là "Lý do {id}". */
    private String answer(String reply, Long... storyIds) {
        List<Map<String, Object>> recommendations = java.util.Arrays.stream(storyIds)
                .map(id -> Map.<String, Object>of("storyId", id, "reason", "Lý do " + id))
                .toList();
        try {
            return objectMapper.writeValueAsString(Map.of("reply", reply, "recommendations", recommendations));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** Tham số của searchStories đúng dạng mô hình thật gửi: record nằm dưới khóa tên tham số "request". */
    private static String searchArgs(String keyword) {
        return "{\"request\": {\"keyword\": \"" + keyword + "\"}}";
    }

    private JsonNode data(String json) {
        try {
            return objectMapper.readTree(json).path("data");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Map<String, Object> assistantRow(long messageId) {
        return jdbcTemplate.queryForMap("SELECT * FROM chat_message WHERE id = ?", messageId);
    }

    private String setting(String key) {
        return jdbcTemplate.queryForObject("SELECT setting_value FROM setting WHERE setting_key = ?", String.class, key);
    }

    private void setSetting(String key, String value) {
        jdbcTemplate.update("UPDATE setting SET setting_value = ? WHERE setting_key = ?", value, key);
        settingService.clearCache();
    }
}
