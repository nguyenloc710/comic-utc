package vn.edu.utc.comic.stats.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tiêu chí của bảng xếp hạng.
 */
@Getter
@RequiredArgsConstructor
public enum RankingType {

    /** Lượt xem trong hôm nay. */
    DAY(1),
    /** Lượt xem trong 7 ngày gần nhất. */
    WEEK(7),
    /** Lượt xem trong 30 ngày gần nhất. */
    MONTH(30),
    /** Tổng lượt theo dõi. */
    FOLLOWS(0),
    /** Điểm đánh giá trung bình. */
    RATING(0);

    /** Số ngày gần nhất được tính (kể cả hôm nay); 0 với tiêu chí không theo lượt xem. */
    private final int viewWindowDays;

    public boolean isByViews() {
        return viewWindowDays > 0;
    }
}
