package vn.edu.utc.comic.story.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.utc.comic.story.entity.Story;

/** Truy vấn truyện; lọc động (tìm kiếm, thể loại, loại, trạng thái) đi qua Specification. */
public interface StoryRepository extends JpaRepository<Story, Long>, JpaSpecificationExecutor<Story> {
}
