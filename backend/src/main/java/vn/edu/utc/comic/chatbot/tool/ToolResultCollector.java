package vn.edu.utc.comic.chatbot.tool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.model.ToolContext;
import vn.edu.utc.comic.chatbot.dto.ToolTraceEntry;

/**
 * Ghi lại mọi lần mô hình gọi hàm trong MỘT lượt hỏi: tham số và id truyện hàm đã trả.
 *
 * <p>Truyền vào hàm qua {@link ToolContext} (mô hình không thấy và không điền được), rồi được đọc lại sau khi mô
 * hình trả lời: tập id ở đây là nguồn cho bước hậu kiểm — gợi ý nào không đến từ kết quả hàm đều bị loại.
 */
public class ToolResultCollector {

    /** Khóa đặt bộ thu trong ToolContext. */
    public static final String CONTEXT_KEY = "toolResultCollector";

    private final List<ToolTraceEntry> entries = new ArrayList<>();

    /** Lấy bộ thu của lượt hiện tại từ ngữ cảnh mà ChatbotService đã đặt khi gọi mô hình. */
    public static ToolResultCollector from(ToolContext toolContext) {
        return (ToolResultCollector) toolContext.getContext().get(CONTEXT_KEY);
    }

    /** Mô hình có thể gọi nhiều hàm song song nên phải đồng bộ. */
    public synchronized void record(String tool, Object arguments, List<Long> storyIds) {
        entries.add(new ToolTraceEntry(tool, arguments, List.copyOf(storyIds)));
    }

    public synchronized List<ToolTraceEntry> entries() {
        return List.copyOf(entries);
    }

    /** Mọi id truyện các hàm đã trả trong lượt này. */
    public synchronized Set<Long> storyIds() {
        Set<Long> ids = new LinkedHashSet<>();
        entries.forEach(entry -> ids.addAll(entry.storyIds()));
        return ids;
    }

    /** Id truyện của lần gọi hàm gần nhất có kết quả, dùng khi mô hình trả lời không đúng định dạng. */
    public synchronized List<Long> lastStoryIds() {
        for (int index = entries.size() - 1; index >= 0; index--) {
            if (!entries.get(index).storyIds().isEmpty()) {
                return entries.get(index).storyIds();
            }
        }
        return List.of();
    }
}
