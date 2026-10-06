package vn.edu.utc.comic.common.job;

import org.springframework.data.jpa.repository.JpaRepository;

/** Truy vấn lịch sử chạy job. */
public interface JobRunRepository extends JpaRepository<JobRun, Long> {
}
