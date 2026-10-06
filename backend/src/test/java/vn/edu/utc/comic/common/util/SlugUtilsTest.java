package vn.edu.utc.comic.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlugUtilsTest {

    private static final int MAX_LENGTH = 120;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Tu tiên | tu-tien",
            "Khoa học viễn tưởng | khoa-hoc-vien-tuong",
            "ĐÔ THỊ | do-thi",
            "Đam mỹ & Bách hợp | dam-my-bach-hop",
            "  Nữ cường!!  | nu-cuong",
            "Top 10 (2026) | top-10-2026",
            "Ẩm thực — Điền văn | am-thuc-dien-van"
    })
    void toSlug_stripsVietnameseDiacriticsAndPunctuation(String name, String expectedSlug) {
        assertThat(SlugUtils.toSlug(name, MAX_LENGTH)).isEqualTo(expectedSlug);
    }

    @Test
    void toSlug_returnsEmpty_whenNameHasNoLetterOrDigit() {
        assertThat(SlugUtils.toSlug("!!! ???", MAX_LENGTH)).isEmpty();
        assertThat(SlugUtils.toSlug("   ", MAX_LENGTH)).isEmpty();
        assertThat(SlugUtils.toSlug(null, MAX_LENGTH)).isEmpty();
    }

    @Test
    void toSlug_cutsToMaxLength_withoutLeavingTrailingHyphen() {
        // Cắt ở 8 ký tự rơi đúng vào gạch ngang sau "tu-tien"
        assertThat(SlugUtils.toSlug("Tu tiên huyền huyễn", 8)).isEqualTo("tu-tien");
    }
}
