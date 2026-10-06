package vn.edu.utc.comic.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.comic.support.AbstractIntegrationTest;

/**
 * Kiểm chứng sớm rủi ro lớn nhất của chatbot (docs/02 mục 1.5): khóa API dùng được, mạng không chặn nhà cung
 * cấp, và mô hình thật sự gọi hàm rồi dùng kết quả hàm để trả lời.
 *
 * <p>Gọi LLM thật nên tốn tiền và cần mạng: chỉ chạy khi có biến môi trường AI_API_KEY, còn lại bị bỏ qua.
 * Hàm ở đây là giả, trả danh sách cứng; hàm thật tra cơ sở dữ liệu được viết ở giai đoạn 6.
 */
@EnabledIfEnvironmentVariable(named = "AI_API_KEY", matches = ".+")
class SpringAiToolCallingSmokeTest extends AbstractIntegrationTest {

    /** Tên bịa không thể có trong dữ liệu huấn luyện: mô hình chỉ biết nó nếu thật sự đọc kết quả hàm. */
    private static final String FAKE_STORY_TITLE = "Vạn Cổ Thanh Đăng Lục 7391";

    @Autowired
    private ChatModel chatModel;

    @Test
    void model_callsToolAndAnswersFromItsResult() {
        FakeCatalogTools tools = new FakeCatalogTools();

        String reply = ChatClient.create(chatModel).prompt()
                .system("Bạn là trợ lý gợi ý truyện. Chỉ gợi ý truyện lấy từ kết quả hàm tìm truyện.")
                .user("Gợi ý cho tôi một truyện tu tiên, nêu đúng tên truyện.")
                .tools(tools)
                .call()
                .content();

        assertThat(tools.requestedGenre.get()).as("mô hình phải gọi hàm tìm truyện").isNotNull();
        assertThat(reply).contains(FAKE_STORY_TITLE);
    }

    static class FakeCatalogTools {

        private final AtomicReference<String> requestedGenre = new AtomicReference<>();

        @Tool(description = "Tìm truyện có trong kho theo thể loại. Trả về danh sách tên truyện.")
        public List<String> searchStories(
                @ToolParam(description = "Thể loại cần tìm, ví dụ: tu tiên, ngôn tình, kinh dị") String genre) {
            requestedGenre.set(genre);
            return List.of(FAKE_STORY_TITLE);
        }
    }
}
