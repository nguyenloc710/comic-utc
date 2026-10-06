package vn.edu.utc.comic.interaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.interaction.entity.StoryFollow;
import vn.edu.utc.comic.interaction.entity.UserStoryId;

/** Truy vấn quan hệ theo dõi truyện. */
public interface StoryFollowRepository extends JpaRepository<StoryFollow, UserStoryId> {
}
