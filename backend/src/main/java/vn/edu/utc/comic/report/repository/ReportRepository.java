package vn.edu.utc.comic.report.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.report.entity.Report;
import vn.edu.utc.comic.report.enums.ReportStatus;
import vn.edu.utc.comic.report.enums.ReportTargetType;

/** Truy vấn báo cáo vi phạm. */
public interface ReportRepository extends JpaRepository<Report, Long> {

    /** Cùng một người đã có báo cáo đang chờ về cùng nội dung này chưa (chặn gửi lặp). */
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, ReportTargetType targetType,
                                                                  Long targetId, ReportStatus status);

    /** Nạp kèm người báo cáo và người xử lý để bảng không phát sinh thêm truy vấn cho mỗi dòng. */
    @EntityGraph(attributePaths = {"reporter", "handledBy"})
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "handledBy"})
    Optional<Report> findWithPeopleById(Long id);

    long countByStatus(ReportStatus status);

    /** Nạp để xử lý và khóa dòng: hai quản trị viên bấm cùng lúc thì người sau thấy "đã được xử lý". */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :reportId")
    Optional<Report> findForHandling(@Param("reportId") Long reportId);
}
