package vn.edu.utc.comic.stats.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.stats.entity.StoryViewDaily;
import vn.edu.utc.comic.stats.entity.StoryViewDailyId;

/** Truy vấn lượt xem theo ngày. */
public interface StoryViewDailyRepository extends JpaRepository<StoryViewDaily, StoryViewDailyId> {

    /**
     * Cộng một lượt xem cho truyện trong ngày. Phải là SQL gốc: JPQL không có "thêm dòng nếu chưa có, cộng dồn
     * nếu đã có" trong một câu lệnh, còn tách thành đọc-rồi-ghi thì hai lượt xem đồng thời sẽ mất một.
     * Tự mở transaction riêng: xem lý do ở {@code ViewCountService}.
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES (:storyId, :viewDate, 1)
            ON DUPLICATE KEY UPDATE view_count = view_count + 1
            """, nativeQuery = true)
    int addDailyView(@Param("storyId") Long storyId, @Param("viewDate") LocalDate viewDate);

    /** Id các truyện công khai được xem nhiều nhất kể từ một ngày, nhiều lượt xem nhất trước. */
    @Query("""
            SELECT v.id.storyId FROM StoryViewDaily v JOIN Story s ON s.id = v.id.storyId
            WHERE v.id.viewDate >= :fromDate
              AND s.visibility = vn.edu.utc.comic.story.enums.StoryVisibility.PUBLISHED AND s.deletedAt IS NULL
            GROUP BY v.id.storyId
            ORDER BY SUM(v.viewCount) DESC, v.id.storyId DESC
            """)
    List<Long> findTopViewedStoryIds(@Param("fromDate") LocalDate fromDate, Pageable pageable);
}
