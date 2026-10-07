package vn.edu.utc.comic.common.util;

import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Làm sạch HTML của chương truyện chữ trước khi lưu.
 *
 * <p>Trình soạn thảo chạy trên trình duyệt của tác giả nên nội dung gửi lên không đáng tin: chỉ giữ lại
 * một danh sách thẻ định dạng văn bản, bỏ MỌI thuộc tính (không còn chỗ cho {@code onclick}, {@code style},
 * {@code href="javascript:..."}) và bỏ hẳn các thẻ khác như {@code script}, {@code img}, {@code iframe}.
 * Nhờ làm sạch lúc LƯU, trang đọc mới được phép in thẳng nội dung này ra bằng th:utext.
 */
public final class HtmlSanitizer {

    /** Đúng các thẻ mà thanh công cụ của trình soạn thảo tạo ra (docs/00 §4.4). */
    private static final Safelist NOVEL_SAFELIST = Safelist.none()
            .addTags("p", "br", "strong", "em", "u", "h2", "h3", "blockquote", "hr");

    /** Không tự xuống dòng, thụt lề: giữ nguyên khoảng trắng tác giả đã gõ. */
    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().prettyPrint(false);

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * @param html nội dung do trình soạn thảo gửi lên; {@code null} được coi là rỗng
     * @return HTML chỉ còn các thẻ được phép, không thuộc tính
     */
    public static String sanitizeNovelHtml(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, "", NOVEL_SAFELIST, OUTPUT_SETTINGS).trim();
    }

    /** Phần chữ người đọc nhìn thấy, đã bỏ thẻ. */
    public static String toPlainText(String html) {
        return html == null ? "" : Jsoup.parse(html).text().trim();
    }

    /** Số từ của nội dung, tính theo khoảng trắng (đúng với tiếng Việt: mỗi âm tiết là một từ đếm được). */
    public static int countWords(String html) {
        String text = toPlainText(html);
        return text.isEmpty() ? 0 : WHITESPACE.split(text).length;
    }

    private HtmlSanitizer() {
        throw new UnsupportedOperationException("Utility class");
    }
}
