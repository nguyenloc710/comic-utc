package vn.edu.utc.comic.author.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.author.dto.AuthorRequestForm;
import vn.edu.utc.comic.author.dto.AuthorRequestResponse;
import vn.edu.utc.comic.author.dto.AuthorRequestStatusResponse;
import vn.edu.utc.comic.author.entity.AuthorProfile;
import vn.edu.utc.comic.author.entity.AuthorRequest;
import vn.edu.utc.comic.author.enums.AuthorRequestStatus;
import vn.edu.utc.comic.author.event.AuthorRequestReviewedEvent;
import vn.edu.utc.comic.author.mapper.AuthorRequestMapper;
import vn.edu.utc.comic.author.repository.AuthorProfileRepository;
import vn.edu.utc.comic.author.repository.AuthorRequestRepository;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.AuthorConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.exception.FieldValidationException.FieldViolation;
import vn.edu.utc.comic.common.security.AccountChangedEvent;
import vn.edu.utc.comic.common.setting.SettingKeys;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Quy trình đăng ký làm tác giả: độc giả gửi yêu cầu, quản trị viên duyệt hoặc từ chối (docs/00 §4.1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorRequestService {

    private static final String FIELD_PEN_NAME = "penName";
    private static final String AUDIT_USER_ID = "userId";
    private static final String AUDIT_PEN_NAME = "penName";
    private static final String SORT_PROPERTY = "id";

    private final AuthorRequestRepository authorRequestRepository;
    private final AuthorProfileRepository authorProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthorRequestMapper authorRequestMapper;
    private final SettingService settingService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** Tình trạng đăng ký tác giả của một tài khoản: yêu cầu gần nhất và có được gửi yêu cầu mới không. */
    @Transactional(readOnly = true)
    public AuthorRequestStatusResponse getStatus(Long userId) {
        UserAccount user = getUser(userId);
        Optional<AuthorRequest> latest = authorRequestRepository.findFirstByUserIdOrderByIdDesc(userId);
        Instant retryAllowedAt = latest.flatMap(this::findRetryTime).orElse(null);
        boolean canSubmit = user.getRole() == Role.USER && latest.map(this::allowsNewRequest).orElse(true);
        return new AuthorRequestStatusResponse(user.getRole(),
                latest.map(authorRequestMapper::toResponse).orElse(null), canSubmit, retryAllowedAt);
    }

    /**
     * Gửi yêu cầu làm tác giả.
     *
     * @throws ApiException             AUTHOR_REQUEST_NOT_ALLOWED nếu không phải độc giả,
     *                                  AUTHOR_REQUEST_ALREADY_PENDING nếu đang có yêu cầu chờ duyệt,
     *                                  AUTHOR_REQUEST_COOLDOWN nếu vừa bị từ chối và chưa hết thời gian chờ
     * @throws FieldValidationException bút danh đã có tác giả dùng hoặc đang được một yêu cầu khác giữ chỗ
     */
    @Transactional
    public void submit(Long userId, AuthorRequestForm form) {
        UserAccount user = getUser(userId);
        validateMaySubmit(user);
        String penName = form.getPenName().trim();
        validatePenNameAvailable(penName, userId);

        AuthorRequest request = new AuthorRequest();
        request.setUser(user);
        request.setPenName(penName);
        request.setIntroduction(form.getIntroduction().trim());
        request.setIntendedType(form.getIntendedType());
        try {
            authorRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            // Hai lần gửi tới cùng lúc (bấm đúp): UNIQUE trên cột sinh pending_user_id chặn lần đến sau
            throw new ApiException(ErrorCode.AUTHOR_REQUEST_ALREADY_PENDING, exception);
        }
        log.info("Tài khoản {} gửi yêu cầu làm tác giả {}", userId, request.getId());
    }

    /**
     * Danh sách yêu cầu theo trạng thái cho màn duyệt. Yêu cầu chờ duyệt xếp cũ trước (ai gửi trước được xét
     * trước); yêu cầu đã xử lý xếp mới trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    @Transactional(readOnly = true)
    public PageResponse<AuthorRequestResponse> searchRequests(AuthorRequestStatus status, int page) {
        Sort.Direction direction = status == AuthorRequestStatus.PENDING ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<AuthorRequest> requests = authorRequestRepository.findByStatus(status,
                PageRequest.of(Math.max(page, 0), ApiConstants.ADMIN_PAGE_SIZE, Sort.by(direction, SORT_PROPERTY)));
        return PageResponse.of(requests, authorRequestMapper::toResponses);
    }

    /**
     * Chi tiết một yêu cầu cho màn duyệt.
     *
     * @throws ApiException AUTHOR_REQUEST_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public AuthorRequestResponse getRequest(Long requestId) {
        return authorRequestRepository.findWithUserById(requestId)
                .map(authorRequestMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTHOR_REQUEST_NOT_FOUND));
    }

    /**
     * Duyệt yêu cầu: trong MỘT transaction đổi trạng thái yêu cầu, tạo hồ sơ tác giả và nâng vai trò tài khoản,
     * nên không bao giờ có tài khoản là tác giả mà thiếu bút danh. Phiên đang đăng nhập của người đó nhận
     * quyền mới ở request kế tiếp (qua {@link AccountChangedEvent}).
     *
     * @return bút danh vừa được cấp
     * @throws ApiException AUTHOR_REQUEST_NOT_FOUND, AUTHOR_REQUEST_ALREADY_REVIEWED nếu đã được xử lý,
     *                      AUTHOR_REQUEST_NOT_ALLOWED nếu người gửi không còn là độc giả,
     *                      AUTHOR_PEN_NAME_TAKEN nếu trong lúc chờ đã có tác giả khác mang bút danh này
     */
    @Transactional
    public String approve(Long requestId, Long reviewerId) {
        AuthorRequest request = getPendingRequest(requestId);
        UserAccount user = request.getUser();
        if (user.getRole() != Role.USER) {
            throw new ApiException(ErrorCode.AUTHOR_REQUEST_NOT_ALLOWED);
        }
        if (authorProfileRepository.existsByPenName(request.getPenName())) {
            throw new ApiException(ErrorCode.AUTHOR_PEN_NAME_TAKEN, request.getPenName());
        }
        Instant now = clock.instant();
        markReviewed(request, AuthorRequestStatus.APPROVED, reviewerId, now);

        AuthorProfile profile = new AuthorProfile();
        profile.setUser(user);
        profile.setPenName(request.getPenName());
        profile.setApprovedAt(now);
        authorProfileRepository.save(profile);
        user.setRole(Role.AUTHOR);

        recordReview(AuditAction.AUTHOR_REQUEST_APPROVED, request);
        // Vai trò nằm trong phiên đăng nhập, phải báo để phiên của người vừa được duyệt được làm mới
        eventPublisher.publishEvent(new AccountChangedEvent(user.getId()));
        eventPublisher.publishEvent(new AuthorRequestReviewedEvent(user.getId(), true, request.getPenName(), null));
        return request.getPenName();
    }

    /**
     * Từ chối yêu cầu kèm lý do. Người gửi đọc được lý do và được gửi lại sau thời gian chờ (đọc từ setting).
     *
     * @throws ApiException AUTHOR_REQUEST_NOT_FOUND, AUTHOR_REQUEST_ALREADY_REVIEWED nếu đã được xử lý
     */
    @Transactional
    public void reject(Long requestId, Long reviewerId, String rejectReason) {
        AuthorRequest request = getPendingRequest(requestId);
        markReviewed(request, AuthorRequestStatus.REJECTED, reviewerId, clock.instant());
        request.setRejectReason(rejectReason.trim());

        recordReview(AuditAction.AUTHOR_REQUEST_REJECTED, request);
        eventPublisher.publishEvent(new AuthorRequestReviewedEvent(request.getUser().getId(), false,
                request.getPenName(), request.getRejectReason()));
    }

    private void validateMaySubmit(UserAccount user) {
        if (user.getRole() != Role.USER) {
            throw new ApiException(ErrorCode.AUTHOR_REQUEST_NOT_ALLOWED);
        }
        Optional<AuthorRequest> latest = authorRequestRepository.findFirstByUserIdOrderByIdDesc(user.getId());
        if (latest.isEmpty() || allowsNewRequest(latest.get())) {
            return;
        }
        if (latest.get().getStatus() == AuthorRequestStatus.REJECTED) {
            throw new ApiException(ErrorCode.AUTHOR_REQUEST_COOLDOWN, getCooldownDays());
        }
        throw new ApiException(ErrorCode.AUTHOR_REQUEST_ALREADY_PENDING);
    }

    /** Chỉ một yêu cầu đã bị từ chối và đã qua thời gian chờ mới cho phép gửi yêu cầu kế tiếp. */
    private boolean allowsNewRequest(AuthorRequest latest) {
        return latest.getStatus() == AuthorRequestStatus.REJECTED
                && findRetryTime(latest).map(retryAt -> !retryAt.isAfter(clock.instant())).orElse(true);
    }

    /** Thời điểm được gửi lại, chỉ có khi yêu cầu đã bị từ chối. */
    private Optional<Instant> findRetryTime(AuthorRequest request) {
        if (request.getStatus() != AuthorRequestStatus.REJECTED || request.getReviewedAt() == null) {
            return Optional.empty();
        }
        return Optional.of(request.getReviewedAt().plus(Duration.ofDays(getCooldownDays())));
    }

    private int getCooldownDays() {
        return settingService.getInt(SettingKeys.AUTHOR_REQUEST_COOLDOWN_DAYS,
                AuthorConstants.DEFAULT_REQUEST_COOLDOWN_DAYS);
    }

    /**
     * Bút danh phải chưa có tác giả nào dùng, và chưa bị yêu cầu đang chờ của người khác giữ chỗ — nếu không,
     * duyệt yêu cầu này xong thì yêu cầu kia chắc chắn không duyệt được nữa.
     */
    private void validatePenNameAvailable(String penName, Long userId) {
        boolean taken = authorProfileRepository.existsByPenName(penName)
                || authorRequestRepository.existsByPenNameAndStatusAndUserIdNot(penName,
                AuthorRequestStatus.PENDING, userId);
        if (taken) {
            throw new FieldValidationException(
                    List.of(new FieldViolation(FIELD_PEN_NAME, MessageKeys.ERROR_PEN_NAME_DUPLICATED)));
        }
    }

    private AuthorRequest getPendingRequest(Long requestId) {
        AuthorRequest request = authorRequestRepository.findForReview(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTHOR_REQUEST_NOT_FOUND));
        if (request.getStatus() != AuthorRequestStatus.PENDING) {
            throw new ApiException(ErrorCode.AUTHOR_REQUEST_ALREADY_REVIEWED);
        }
        return request;
    }

    private void markReviewed(AuthorRequest request, AuthorRequestStatus status, Long reviewerId, Instant now) {
        request.setStatus(status);
        request.setReviewedBy(userAccountRepository.getReferenceById(reviewerId));
        request.setReviewedAt(now);
    }

    private void recordReview(AuditAction action, AuthorRequest request) {
        auditService.recordForCurrentUser(action, AuditedEntity.of(AuthorRequest.class, request.getId()),
                Map.of(AUDIT_USER_ID, request.getUser().getId(), AUDIT_PEN_NAME, request.getPenName()));
        log.info("{} yêu cầu làm tác giả {}", action, request.getId());
    }

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
