package vn.edu.utc.comic.author.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.author.entity.AuthorProfile;

/** Truy vấn hồ sơ tác giả. */
public interface AuthorProfileRepository extends JpaRepository<AuthorProfile, Long> {

    /** So sánh không phân biệt hoa thường và dấu nhờ collation utf8mb4_unicode_ci của cột. */
    boolean existsByPenName(String penName);
}
