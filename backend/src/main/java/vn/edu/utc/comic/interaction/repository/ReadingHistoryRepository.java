package vn.edu.utc.comic.interaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.interaction.entity.ReadingHistory;
import vn.edu.utc.comic.interaction.entity.UserStoryId;

/** Truy vấn tiến độ đọc. */
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, UserStoryId> {
}
