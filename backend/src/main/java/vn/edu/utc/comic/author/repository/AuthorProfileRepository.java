package vn.edu.utc.comic.author.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.author.entity.AuthorProfile;

/** Truy vấn hồ sơ tác giả. */
public interface AuthorProfileRepository extends JpaRepository<AuthorProfile, Long> {
}
