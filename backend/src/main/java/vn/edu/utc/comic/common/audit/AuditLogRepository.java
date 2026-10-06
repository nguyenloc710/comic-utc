package vn.edu.utc.comic.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/** Truy vấn nhật ký kiểm toán. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
