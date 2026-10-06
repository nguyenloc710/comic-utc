package vn.edu.utc.comic.chapter.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Trạng thái chương. Bảng chuyển đổi hợp lệ nằm ở đây, service chỉ hỏi (docs/00 §4.3).
 *
 * <p>DRAFT: tác giả đang soạn. SCHEDULED: chờ tới giờ hẹn để job đăng. PUBLISHED: người đọc thấy.
 * HIDDEN: bị quản trị viên ẩn kèm lý do.
 */
public enum ChapterStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHED,
    HIDDEN;

    private static final Map<ChapterStatus, Set<ChapterStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(SCHEDULED, PUBLISHED),
            SCHEDULED, EnumSet.of(DRAFT, PUBLISHED),
            PUBLISHED, EnumSet.of(HIDDEN),
            HIDDEN, EnumSet.of(PUBLISHED));

    public boolean canTransitionTo(ChapterStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    /** Chỉ chương đã đăng mới tính vào số chương của truyện và hiện với người đọc. */
    public boolean isPubliclyReadable() {
        return this == PUBLISHED;
    }
}
