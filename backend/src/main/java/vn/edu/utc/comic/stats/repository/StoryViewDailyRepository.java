package vn.edu.utc.comic.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.stats.entity.StoryViewDaily;
import vn.edu.utc.comic.stats.entity.StoryViewDailyId;

/** Truy vấn lượt xem theo ngày. */
public interface StoryViewDailyRepository extends JpaRepository<StoryViewDaily, StoryViewDailyId> {
}
