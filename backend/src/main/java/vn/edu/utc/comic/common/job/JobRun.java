package vn.edu.utc.comic.common.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.BaseEntity;

/** Một lượt chạy của job định kỳ (đăng chương hẹn giờ, dọn hội thoại chatbot) để theo dõi và chẩn đoán. */
@Getter
@Setter
@Entity
@Table(name = "job_run")
public class JobRun extends BaseEntity {

    @Column(name = "job_name", nullable = false, length = 50)
    private String jobName;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private JobStatus status;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    /** Số phần tử lỗi trong lượt chạy; một phần tử lỗi không làm dừng cả lượt. */
    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
