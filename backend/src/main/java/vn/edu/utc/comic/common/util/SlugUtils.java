package vn.edu.utc.comic.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sinh slug (định danh trên URL) từ tên tiếng Việt: bỏ dấu, chữ thường, nối bằng gạch ngang.
 */
public final class SlugUtils {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");
    private static final String SEPARATOR = "-";

    /**
     * @param text      tên gốc, ví dụ "Khoa học viễn tưởng"
     * @param maxLength độ dài tối đa của slug (theo độ dài cột)
     * @return slug, ví dụ "khoa-hoc-vien-tuong"; chuỗi rỗng nếu tên không chứa chữ hay số nào
     */
    public static String toSlug(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        // Chữ "đ" không phải chữ "d" kèm dấu nên chuẩn hóa NFD không tách được, phải đổi tay
        String lowerCased = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        String withoutMarks = COMBINING_MARKS.matcher(Normalizer.normalize(lowerCased, Normalizer.Form.NFD))
                .replaceAll("");
        String hyphenated = NON_ALPHANUMERIC.matcher(withoutMarks).replaceAll(SEPARATOR);
        String trimmed = EDGE_HYPHENS.matcher(hyphenated).replaceAll("");
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        // Cắt theo độ dài cột có thể để lại gạch ngang ở cuối
        return EDGE_HYPHENS.matcher(trimmed.substring(0, maxLength)).replaceAll("");
    }

    private SlugUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
