package vn.edu.utc.comic.report.service;

import java.time.Clock;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.chapter.repository.ChapterRepository;
import vn.edu.utc.comic.chapter.service.ChapterPublishService;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.common.util.StoryLinks;
import vn.edu.utc.comic.interaction.enums.CommentStatus;
import vn.edu.utc.comic.interaction.repository.CommentRepository;
import vn.edu.utc.comic.interaction.service.CommentModerationService;
import vn.edu.utc.comic.report.dto.ReportCreateRequest;
import vn.edu.utc.comic.report.dto.ReportResponse;
import vn.edu.utc.comic.report.dto.ReportTarget;
import vn.edu.utc.comic.report.entity.Report;
import vn.edu.utc.comic.report.enums.ReportStatus;
import vn.edu.utc.comic.report.enums.ReportTargetType;
import vn.edu.utc.comic.report.mapper.ReportMapper;
import vn.edu.utc.comic.report.repository.ReportRepository;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.service.StoryAccessPolicy;
import vn.edu.utc.comic.story.service.StoryModerationService;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Báo cáo vi phạm: độc giả gửi, quản trị viên xử lý (bỏ qua hoặc ẩn nội dung).
 *
 * <p>Việc ẩn nội dung KHÔNG làm ở đây mà đi qua đúng service kiểm duyệt của từng loại, để bộ đếm, thông báo
 * cho tác giả và nhật ký kiểm toán được ghi y như khi quản trị viên ẩn trực tiếp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String SORT_PROPERTY = "id";
    private static final String AUDIT_TARGET = "target";
    private static final String AUDIT_NOTE = "note";

    private final ReportRepository reportRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final CommentRepository commentRepository;
    private final UserAccountRepository userAccountRepository;
    private final StoryAccessPolicy accessPolicy;
    private final StoryModerationService storyModerationService;
    private final ChapterPublishService chapterPublishService;
    private final CommentModerationService commentModerationService;
    private final ReportMapper reportMapper;
    private final AuditService auditService;
    private final Clock clock;

    /**
     * Độc giả gửi báo cáo về một nội dung đang hiển thị công khai.
     *
     * @throws ApiException REPORT_TARGET_NOT_FOUND nếu nội dung không tồn tại hoặc không công khai (không lộ
     *                      bản nháp); REPORT_ALREADY_PENDING nếu người này đã báo cáo nội dung này và chưa được xử lý
     */
    @Transactional
    public void submit(Long reporterId, ReportCreateRequest request) {
        ReportTarget target = resolveTarget(request.targetType(), request.targetId());
        if (target.hidden()) {
            throw new ApiException(ErrorCode.REPORT_TARGET_NOT_FOUND);
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(reporterId, request.targetType(),
                request.targetId(), ReportStatus.PENDING)) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_PENDING);
        }
        Report report = new Report();
        report.setReporter(userAccountRepository.getReferenceById(reporterId));
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReason(request.reason());
        report.setDetail(request.detail() == null || request.detail().isBlank() ? null : request.detail().trim());
        reportRepository.save(report);
        log.info("Tài khoản {} báo cáo {} {}", reporterId, request.targetType(), request.targetId());
    }

    /**
     * Hàng đợi báo cáo theo trạng thái: báo cáo chờ xếp cũ trước, báo cáo đã xử lý xếp mới trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> searchReports(ReportStatus status, int page) {
        Sort.Direction direction = status == ReportStatus.PENDING ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Report> reports = reportRepository.findByStatus(status,
                PageRequest.of(Math.max(page, 0), ApiConstants.ADMIN_PAGE_SIZE, Sort.by(direction, SORT_PROPERTY)));
        return PageResponse.of(reports, content -> content.stream().map(this::toResponse).toList());
    }

    /**
     * @throws ApiException REPORT_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public ReportResponse getReport(Long reportId) {
        return toResponse(reportRepository.findWithPeopleById(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.REPORT_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    /**
     * Bỏ qua báo cáo (nội dung không vi phạm).
     *
     * @param note ghi chú của quản trị viên, có thể bỏ trống
     * @throws ApiException REPORT_NOT_FOUND; REPORT_ALREADY_HANDLED
     */
    @Transactional
    public void dismiss(Long reportId, Long adminId, String note) {
        Report report = getPendingReport(reportId);
        markHandled(report, ReportStatus.DISMISSED, adminId, note);
        auditService.recordForCurrentUser(AuditAction.REPORT_DISMISSED, AuditedEntity.of(Report.class, reportId),
                Map.of(AUDIT_TARGET, report.getTargetType() + ":" + report.getTargetId()));
    }

    /**
     * Xác nhận vi phạm: ẩn nội dung (qua service kiểm duyệt tương ứng, với lý do là ghi chú này) rồi đóng báo cáo.
     * Nội dung đã bị ẩn từ trước (do một báo cáo khác) thì chỉ đóng báo cáo.
     *
     * @param reason lý do gửi tới tác giả, bắt buộc
     * @throws ApiException REPORT_NOT_FOUND; REPORT_ALREADY_HANDLED
     */
    @Transactional
    public void resolve(Long reportId, Long adminId, String reason) {
        Report report = getPendingReport(reportId);
        if (!resolveTarget(report.getTargetType(), report.getTargetId()).hidden()) {
            hideTarget(report, reason);
        }
        markHandled(report, ReportStatus.RESOLVED, adminId, reason);
        auditService.recordForCurrentUser(AuditAction.REPORT_RESOLVED, AuditedEntity.of(Report.class, reportId),
                Map.of(AUDIT_TARGET, report.getTargetType() + ":" + report.getTargetId(), AUDIT_NOTE, reason));
    }

    private void hideTarget(Report report, String reason) {
        switch (report.getTargetType()) {
            case STORY -> storyModerationService.hideStory(report.getTargetId(), reason);
            case CHAPTER -> chapterPublishService.hideChapter(report.getTargetId(), reason);
            case COMMENT -> commentModerationService.hideComment(report.getTargetId(), reason);
        }
    }

    private Report getPendingReport(Long reportId) {
        Report report = reportRepository.findForHandling(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.REPORT_NOT_FOUND));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_HANDLED);
        }
        return report;
    }

    private void markHandled(Report report, ReportStatus status, Long adminId, String note) {
        report.setStatus(status);
        report.setHandledBy(userAccountRepository.getReferenceById(adminId));
        report.setHandledAt(clock.instant());
        report.setResolutionNote(note == null || note.isBlank() ? null : note.trim());
    }

    private ReportResponse toResponse(Report report) {
        return reportMapper.toResponse(report, resolveTarget(report.getTargetType(), report.getTargetId()));
    }

    /** Tra nội dung bị báo cáo; "ẩn" ở đây nghĩa là người đọc thường không còn thấy nó (ẩn, nháp hoặc đã xóa). */
    private ReportTarget resolveTarget(ReportTargetType type, Long targetId) {
        return switch (type) {
            case STORY -> storyRepository.findById(targetId)
                    .map(story -> new ReportTarget(story.getTitle(), StoryLinks.story(story.getSlug()),
                            !accessPolicy.canView(story, Viewer.anonymous())))
                    .orElseGet(ReportTarget::missing);
            case CHAPTER -> chapterRepository.findWithStoryById(targetId)
                    .map(chapter -> new ReportTarget(chapter.getChapterNo() + " – " + chapter.getStory().getTitle(),
                            StoryLinks.chapter(chapter.getStory().getSlug(), chapter.getChapterNo()),
                            !accessPolicy.isPubliclyReadable(chapter, chapter.getStory())))
                    .orElseGet(ReportTarget::missing);
            case COMMENT -> commentRepository.findById(targetId)
                    .map(comment -> new ReportTarget(CommentModerationService.snippetOf(comment),
                            StoryLinks.story(comment.getStory().getSlug()) + StoryLinks.COMMENTS_ANCHOR,
                            comment.getStatus() != CommentStatus.VISIBLE
                                    || !accessPolicy.canView(comment.getStory(), Viewer.anonymous())))
                    .orElseGet(ReportTarget::missing);
        };
    }
}
