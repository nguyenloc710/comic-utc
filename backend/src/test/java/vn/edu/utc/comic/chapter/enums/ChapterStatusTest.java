package vn.edu.utc.comic.chapter.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.edu.utc.comic.chapter.enums.ChapterStatus.DRAFT;
import static vn.edu.utc.comic.chapter.enums.ChapterStatus.HIDDEN;
import static vn.edu.utc.comic.chapter.enums.ChapterStatus.PUBLISHED;
import static vn.edu.utc.comic.chapter.enums.ChapterStatus.SCHEDULED;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Bảng chuyển trạng thái chương ở docs/00 §4.3, kiểm tra toàn bộ 16 cặp (từ, sang). */
class ChapterStatusTest {

    private static final Map<ChapterStatus, Set<ChapterStatus>> ALLOWED = Map.of(
            DRAFT, Set.of(SCHEDULED, PUBLISHED),
            SCHEDULED, Set.of(DRAFT, PUBLISHED),
            PUBLISHED, Set.of(HIDDEN),
            HIDDEN, Set.of(PUBLISHED));

    @ParameterizedTest
    @EnumSource(ChapterStatus.class)
    void canTransitionTo_matchesDocumentedTable(ChapterStatus from) {
        for (ChapterStatus target : ChapterStatus.values()) {
            assertThat(from.canTransitionTo(target))
                    .as("%s -> %s", from, target)
                    .isEqualTo(ALLOWED.get(from).contains(target));
        }
    }

    @Test
    void publishedChapter_cannotGoBackToDraft() {
        // Người theo dõi đã nhận thông báo và chương đã tính vào số chương: không có đường quay lại bản nháp
        assertThat(PUBLISHED.canTransitionTo(DRAFT)).isFalse();
        assertThat(PUBLISHED.canTransitionTo(SCHEDULED)).isFalse();
    }

    @Test
    void onlyPublishedChapter_isPubliclyReadable() {
        assertThat(ChapterStatus.values()).filteredOn(ChapterStatus::isPubliclyReadable).containsExactly(PUBLISHED);
    }
}
