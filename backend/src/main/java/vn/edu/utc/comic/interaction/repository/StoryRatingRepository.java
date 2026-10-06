package vn.edu.utc.comic.interaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.interaction.entity.StoryRating;
import vn.edu.utc.comic.interaction.entity.UserStoryId;

/** Truy vấn đánh giá sao. */
public interface StoryRatingRepository extends JpaRepository<StoryRating, UserStoryId> {
}
