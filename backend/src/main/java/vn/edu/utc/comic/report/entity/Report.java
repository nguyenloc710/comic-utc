package vn.edu.utc.comic.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.CreatedAtEntity;
import vn.edu.utc.comic.report.enums.ReportReason;
import vn.edu.utc.comic.report.enums.ReportStatus;
import vn.edu.utc.comic.report.enums.ReportTargetType;
import vn.edu.utc.comic.user.entity.UserAccount;

/**
 * Báo cáo vi phạm của độc giả về một truyện, chương hoặc bình luận.
 * Đối tượng là đa hình (target_type + target_id) nên không có khóa ngoại; service kiểm tra tồn tại.
 */
@Getter
@Setter
@Entity
@Table(name = "report")
public class Report extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserAccount reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private UserAccount handledBy;

    @Column(name = "handled_at")
    private Instant handledAt;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;
}
