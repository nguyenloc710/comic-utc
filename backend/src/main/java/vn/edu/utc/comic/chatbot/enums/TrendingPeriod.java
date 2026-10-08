package vn.edu.utc.comic.chatbot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.edu.utc.comic.stats.enums.RankingType;

/** Khoảng thời gian của hàm "truyện đang hot", ánh xạ sang bảng xếp hạng theo lượt xem có sẵn. */
@Getter
@RequiredArgsConstructor
public enum TrendingPeriod {
    DAY(RankingType.DAY),
    WEEK(RankingType.WEEK),
    MONTH(RankingType.MONTH);

    private final RankingType rankingType;
}
