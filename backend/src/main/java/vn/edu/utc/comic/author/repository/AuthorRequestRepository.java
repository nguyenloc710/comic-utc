package vn.edu.utc.comic.author.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.author.entity.AuthorRequest;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;

/** Truy vấn yêu cầu đăng ký tác giả. */
public interface AuthorRequestRepository extends JpaRepository<AuthorRequest, Long> {

    /** Yêu cầu gần nhất của một người (id tự tăng nên id lớn nhất là mới nhất). */
    Optional<AuthorRequest> findFirstByUserIdOrderByIdDesc(Long userId);

    /** Bút danh đang được một yêu cầu chờ duyệt của NGƯỜI KHÁC giữ chỗ hay không. */
    boolean existsByPenNameAndStatusAndUserIdNot(String penName, AuthorRequestStatus status, Long userId);

    /** Nạp kèm người gửi để bảng danh sách không phát sinh thêm một truy vấn cho mỗi dòng. */
    @EntityGraph(attributePaths = "user")
    Page<AuthorRequest> findByStatus(AuthorRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<AuthorRequest> findWithUserById(Long id);

    /**
     * Nạp yêu cầu để duyệt / từ chối và khóa dòng tới hết transaction: hai quản trị viên bấm cùng lúc thì người
     * đến sau phải chờ, rồi thấy yêu cầu đã được xử lý thay vì cùng tạo hồ sơ tác giả hai lần.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AuthorRequest r WHERE r.id = :requestId")
    Optional<AuthorRequest> findForReview(@Param("requestId") Long requestId);

    long countByStatus(AuthorRequestStatus status);
}
