package vn.edu.utc.comic.author.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.author.entity.AuthorRequest;

/** Truy vấn yêu cầu đăng ký tác giả. */
public interface AuthorRequestRepository extends JpaRepository<AuthorRequest, Long> {
}
