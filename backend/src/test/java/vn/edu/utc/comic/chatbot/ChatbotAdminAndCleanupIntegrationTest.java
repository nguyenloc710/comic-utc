package vn.edu.utc.comic.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.chatbot.dto.ChatMessageRequest;
import vn.edu.utc.comic.chatbot.job.ChatCleanupJob;
import vn.edu.utc.comic.chatbot.service.ChatbotService;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.support.ScriptedChatModel;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Trang quản trị chatbot, job dọn hội thoại cũ và hạn mức ngày khi gửi đồng thời.
 */
class ChatbotAdminAndCleanupIntegrationTest extends AbstractIntegrationTest {

    private static final String LIMIT_KEY = "chat.daily_limit_per_user";
    private static final int CONCURRENT_SENDS = 6;

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ChatCleanupJob chatCleanupJob;

    @Autowired
    private ScriptedChatModel chatModel;

    @Autowired
    private SettingService settingService;

    @AfterEach
    void resetModel() {
        chatModel.reset();
    }

    @Test
    void adminPage_showsUsageAndRecentNegativeFeedback() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        long messageId = chatbotService.reply(reader.getId(), new ChatMessageRequest(null, "câu hỏi")).messageId();
        String unhelpful = "Câu trả lời dở " + uniqueSuffix();
        jdbcTemplate.update("UPDATE chat_message SET feedback = -1, content = ? WHERE id = ?", unhelpful, messageId);

        mockMvc.perform(get("/admin/chatbot").with(user(principalOf(Role.USER)))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/chatbot").with(user(principalOf(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewConstants.ADMIN_CHATBOT))
                .andExpect(content().string(containsString(unhelpful)));
    }

    @Test
    void cleanupJob_deletesOnlyConversationsInactiveLongerThanTheRetention() {
        UserAccount reader = createAccount(Role.USER);
        long oldConversation = chatbotService.reply(reader.getId(), new ChatMessageRequest(null, "cũ")).conversationId();
        long recentConversation = chatbotService.reply(reader.getId(), new ChatMessageRequest(null, "mới")).conversationId();
        jdbcTemplate.update("UPDATE chat_conversation SET last_message_at = ? WHERE id = ?",
                utc(Instant.now().minus(91, ChronoUnit.DAYS)), oldConversation);

        int deleted = chatCleanupJob.deleteExpiredConversations();

        assertThat(deleted).isGreaterThanOrEqualTo(1);
        assertThat(countRows("chat_conversation", "id", oldConversation)).isZero();
        assertThat(countRows("chat_message", "conversation_id", oldConversation)).isZero();
        assertThat(countRows("chat_conversation", "id", recentConversation)).isEqualTo(1);
        assertThat(countRows("chat_message", "conversation_id", recentConversation)).isEqualTo(2);
    }

    @Test
    void dailyLimit_holdsEvenWhenOneUserSendsFromSeveralTabsAtOnce() throws Exception {
        UserAccount reader = createAccount(Role.USER);
        String originalLimit = jdbcTemplate.queryForObject("SELECT setting_value FROM setting WHERE setting_key = ?",
                String.class, LIMIT_KEY);
        jdbcTemplate.update("UPDATE setting SET setting_value = '3' WHERE setting_key = ?", LIMIT_KEY);
        settingService.clearCache();
        CountDownLatch startTogether = new CountDownLatch(1);
        int accepted = 0;
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_SENDS)) {
            List<Future<Boolean>> sends = java.util.stream.IntStream.range(0, CONCURRENT_SENDS)
                    .mapToObj(index -> executor.submit(() -> {
                        startTogether.await();
                        try {
                            chatbotService.reply(reader.getId(), new ChatMessageRequest(null, "tab " + index));
                            return true;
                        } catch (ApiException exception) {
                            return false;
                        }
                    }))
                    .toList();
            startTogether.countDown();
            for (Future<Boolean> send : sends) {
                if (send.get(20, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
        } finally {
            jdbcTemplate.update("UPDATE setting SET setting_value = ? WHERE setting_key = ?", originalLimit, LIMIT_KEY);
            settingService.clearCache();
        }

        assertThat(accepted).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM chat_message m JOIN chat_conversation c ON c.id = m.conversation_id
                WHERE c.user_id = ? AND m.role = 'USER'
                """, Long.class, reader.getId())).isEqualTo(3);
    }

    private long countRows(String table, String column, long value) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, value);
    }
}
