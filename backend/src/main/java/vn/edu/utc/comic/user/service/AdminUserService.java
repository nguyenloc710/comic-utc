package vn.edu.utc.comic.user.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.security.AccountChangedEvent;
import vn.edu.utc.comic.common.security.SecurityUtils;
import vn.edu.utc.comic.user.dto.UserFilterRequest;
import vn.edu.utc.comic.user.dto.UserSummaryResponse;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.UserStatus;
import vn.edu.utc.comic.user.mapper.UserMapper;
import vn.edu.utc.comic.user.repository.UserAccountRepository;
import vn.edu.utc.comic.user.repository.UserAccountSpecification;

/**
 * Quản trị tài khoản: danh sách, khóa, mở khóa. Tài khoản không bao giờ bị xóa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String AUDIT_USERNAME = "username";
    private static final String SORT_PROPERTY = "id";

    private final UserAccountRepository userAccountRepository;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Danh sách tài khoản theo bộ lọc, mới nhất trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(UserFilterRequest filter, int page) {
        // Cỡ trang và cột sắp xếp do máy chủ quyết định, không nhận từ query string
        Pageable pageable = PageRequest.of(Math.max(page, 0), ApiConstants.ADMIN_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, SORT_PROPERTY));
        Page<UserAccount> users = userAccountRepository.findAll(UserAccountSpecification.forFilter(filter), pageable);
        Instant now = clock.instant();
        return PageResponse.of(users, content -> userMapper.toSummaries(content, now));
    }

    /**
     * Khóa tài khoản: không đăng nhập được nữa, phiên đang mở bị hủy ở request kế tiếp.
     * Truyện của tác giả bị khóa KHÔNG tự ẩn; việc đó do quản trị viên quyết định riêng.
     *
     * @return tên đăng nhập của tài khoản vừa khóa
     * @throws ApiException USER_NOT_FOUND nếu không có tài khoản, USER_SELF_BAN nếu tự khóa chính mình
     */
    @Transactional
    public String banUser(Long userId) {
        UserAccount user = getUser(userId);
        // Chặn tự khóa cũng đồng thời bảo đảm hệ thống luôn còn ít nhất một quản trị viên hoạt động
        if (Objects.equals(userId, SecurityUtils.getCurrentUserId())) {
            throw new ApiException(ErrorCode.USER_SELF_BAN);
        }
        user.setStatus(UserStatus.BANNED);
        recordStatusChange(AuditAction.USER_BANNED, user);
        return user.getUsername();
    }

    /**
     * Mở khóa tài khoản, đồng thời gỡ luôn khóa tạm do nhập sai mật khẩu (nếu có).
     *
     * @return tên đăng nhập của tài khoản vừa mở khóa
     * @throws ApiException USER_NOT_FOUND nếu không có tài khoản
     */
    @Transactional
    public String unbanUser(Long userId) {
        UserAccount user = getUser(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        recordStatusChange(AuditAction.USER_UNBANNED, user);
        return user.getUsername();
    }

    private void recordStatusChange(AuditAction action, UserAccount user) {
        auditService.recordForCurrentUser(action, AuditedEntity.of(UserAccount.class, user.getId()),
                Map.of(AUDIT_USERNAME, user.getUsername()));
        eventPublisher.publishEvent(new AccountChangedEvent(user.getId()));
        log.info("{} tài khoản {}", action, user.getId());
    }

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
