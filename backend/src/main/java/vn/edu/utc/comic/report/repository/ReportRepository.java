package vn.edu.utc.comic.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.comic.report.entity.Report;

/** Truy vấn báo cáo vi phạm. */
public interface ReportRepository extends JpaRepository<Report, Long> {
}
