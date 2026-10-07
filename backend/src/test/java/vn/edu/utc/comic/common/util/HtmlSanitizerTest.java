package vn.edu.utc.comic.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HtmlSanitizerTest {

    @Test
    void keepsFormattingTagsOfTheEditor() {
        String html = "<h2>Mở đầu</h2><p>Trời <strong>mưa</strong> <em>rất</em> <u>to</u>.<br>Hết.</p>"
                + "<blockquote>Lời dẫn</blockquote><hr><h3>Phần hai</h3>";

        assertThat(HtmlSanitizer.sanitizeNovelHtml(html)).isEqualTo(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert(1)>",
            "<iframe src=\"https://evil.example\"></iframe>",
            "<svg onload=alert(1)></svg>",
            "<style>body{display:none}</style>",
    })
    void removesDangerousElementsCompletely(String payload) {
        assertThat(HtmlSanitizer.sanitizeNovelHtml("<p>Trước</p>" + payload + "<p>Sau</p>"))
                .isEqualTo("<p>Trước</p><p>Sau</p>");
    }

    @Test
    void stripsEveryAttribute_soNoHandlerOrStyleSurvives() {
        String cleaned = HtmlSanitizer.sanitizeNovelHtml(
                "<p onclick=\"steal()\" style=\"position:fixed\" class=\"x\">Đoạn văn</p>");

        assertThat(cleaned).isEqualTo("<p>Đoạn văn</p>");
    }

    @Test
    void dropsDisallowedTagsButKeepsTheirText() {
        String cleaned = HtmlSanitizer.sanitizeNovelHtml(
                "<p>Xem <a href=\"javascript:alert(1)\">ở đây</a> và <span>chỗ này</span></p>");

        assertThat(cleaned).isEqualTo("<p>Xem ở đây và chỗ này</p>");
    }

    @Test
    void nullOrBlankInput_becomesEmptyContent() {
        assertThat(HtmlSanitizer.sanitizeNovelHtml(null)).isEmpty();
        assertThat(HtmlSanitizer.sanitizeNovelHtml("   ")).isEmpty();
        // Trình soạn thảo để lại một đoạn rỗng khi tác giả chưa gõ gì
        assertThat(HtmlSanitizer.toPlainText("<p><br></p>")).isEmpty();
    }

    @Test
    void countsWordsOfVisibleTextOnly() {
        assertThat(HtmlSanitizer.countWords("<h2>Chương một</h2><p>Trời <strong>mưa</strong> rất to.</p>"))
                .isEqualTo(6);
        assertThat(HtmlSanitizer.countWords("<p><br></p>")).isZero();
        assertThat(HtmlSanitizer.countWords(null)).isZero();
    }
}
