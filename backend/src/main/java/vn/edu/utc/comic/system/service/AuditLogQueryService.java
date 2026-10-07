package vn.edu.utc.comic.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.audit.AuditLog;
import vn.edu.utc.comic.common.audit.AuditLogRepository;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.system.dto.AuditLogFilterRequest;
import vn.edu.utc.comic.system.dto.AuditLogResponse;
import vn.edu.utc.comic.system.mapper.SystemMapper;

/** Đọc nhật ký kiểm toán cho trang quản trị. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final SystemMapper systemMapper;

    /**
     * Nhật ký theo bộ lọc, mới nhất trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    public PageResponse<AuditLogResponse> searchLogs(AuditLogFilterRequest filter, int page) {
        String actor = filter.actor() == null || filter.actor().isBlank() ? null : filter.actor().trim();
        Page<AuditLog> logs = auditLogRepository.findForAdmin(filter.action(), actor,
                PageRequest.of(Math.max(page, 0), ApiConstants.ADMIN_PAGE_SIZE));
        return PageResponse.of(logs, systemMapper::toAuditLogResponses);
    }
}
