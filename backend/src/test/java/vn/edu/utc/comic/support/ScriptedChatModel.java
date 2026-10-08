package vn.edu.utc.comic.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

/**
 * Mô hình ngôn ngữ giả cho test: không gọi mạng, chạy theo kịch bản mỗi test tự đặt.
 *
 * <p>Kịch bản nhận prompt thật mà ChatClient dựng (system prompt, lịch sử, hàm đã đăng ký) và có thể gọi các hàm
 * tra cứu THẬT bằng {@link #callTool}: nhờ vậy test đi qua đúng đường mà mô hình thật đi — hàm, bộ thu kết quả,
 * hậu kiểm — chỉ thay phần "mô hình quyết định gì".
 */
public class ScriptedChatModel implements ChatModel {

    private volatile Function<Prompt, String> script = prompt -> "{\"reply\": \"Xin chào\", \"recommendations\": []}";
    private final List<Prompt> prompts = new ArrayList<>();

    /** Đặt kịch bản: nhận prompt, trả văn bản mô hình "viết" (thường là JSON), hoặc ném lỗi để giả lập sự cố. */
    public void respond(Function<Prompt, String> nextScript) {
        this.script = nextScript;
    }

    /** Các prompt đã nhận, để test kiểm tra mô hình được gửi gì. */
    public synchronized List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public synchronized void reset() {
        prompts.clear();
        respond(prompt -> "{\"reply\": \"Xin chào\", \"recommendations\": []}");
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        synchronized (this) {
            prompts.add(prompt);
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(script.apply(prompt)))));
    }

    /**
     * ChatClient chỉ gắn hàm vào prompt khi tùy chọn mặc định của mô hình là loại hỗ trợ gọi hàm; tùy chọn mặc
     * định của interface ChatModel thì không, nên phải khai báo lại.
     */
    @Override
    public ChatOptions getDefaultOptions() {
        return DefaultToolCallingChatOptions.builder().build();
    }

    /** Gọi một hàm đã đăng ký với tham số JSON như mô hình thật làm, trả kết quả JSON của hàm. */
    public static String callTool(Prompt prompt, String toolName, String argumentsJson) {
        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
        ToolCallback callback = options.getToolCallbacks().stream()
                .filter(candidate -> candidate.getToolDefinition().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hàm chưa được đăng ký: " + toolName));
        return callback.call(argumentsJson, new ToolContext(options.getToolContext()));
    }
}
