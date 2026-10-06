package vn.edu.utc.comic.interaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.interaction.entity.Comment;

/** Truy vấn bình luận. */
public interface CommentRepository extends JpaRepository<Comment, Long> {
}
