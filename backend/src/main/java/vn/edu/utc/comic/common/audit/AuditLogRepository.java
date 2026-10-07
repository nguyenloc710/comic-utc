package vn.edu.utc.comic.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy vấn nhật ký kiểm toán. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Nhật ký cho trang quản trị, mới nhất trước; tham số null nghĩa là không lọc theo tiêu chí đó. */
    @Query(value = """
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actor IS NULL OR a.actorName LIKE CONCAT('%', :actor, '%'))
            ORDER BY a.id DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actor IS NULL OR a.actorName LIKE CONCAT('%', :actor, '%'))
            """)
    Page<AuditLog> findForAdmin(@Param("action") AuditAction action, @Param("actor") String actor, Pageable pageable);
}
