package vn.edu.utc.comic.auth.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.auth.dto.ChangePasswordForm;
import vn.edu.utc.comic.auth.dto.ProfileForm;
import vn.edu.utc.comic.auth.dto.ProfileResponse;
import vn.edu.utc.comic.auth.mapper.ProfileMapper;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.constant.StorageConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.exception.FieldValidationException.FieldViolation;
import vn.edu.utc.comic.common.security.AccountChangedEvent;
import vn.edu.utc.comic.common.storage.StorageCleanup;
import vn.edu.utc.comic.common.storage.StorageService;
import vn.edu.utc.comic.common.storage.StoredFile;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.repository.UserAccountRepository;
import vn.edu.utc.comic.user.service.PasswordPolicy;

/**
 * Hồ sơ cá nhân và đổi mật khẩu của người đang đăng nhập.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final String FIELD_CURRENT_PASSWORD = "currentPassword";
    private static final String FIELD_NEW_PASSWORD = "newPassword";
    private static final String FIELD_CONFIRM_PASSWORD = "confirmPassword";

    private final UserAccountRepository userAccountRepository;
    private final ProfileMapper profileMapper;
    private final StorageService storageService;
    private final StorageCleanup storageCleanup;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    /** Thông tin hiển thị ở đầu trang hồ sơ. */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        UserAccount user = getUser(userId);
        return profileMapper.toResponse(user, storageService.resolveUrl(user.getAvatarPath()));
    }

    /** Form sửa hồ sơ điền sẵn giá trị hiện tại. */
    @Transactional(readOnly = true)
    public ProfileForm getProfileForm(Long userId) {
        return profileMapper.toForm(getUser(userId));
    }

    /**
     * Cập nhật tên hiển thị, giới thiệu và (nếu có chọn tệp) ảnh đại diện.
     *
     * @throws ApiException IMAGE_TYPE_NOT_ALLOWED / IMAGE_TOO_LARGE nếu ảnh không hợp lệ — khi đó không có
     *                      gì được ghi
     */
    @Transactional
    public void updateProfile(Long userId, ProfileForm form) {
        UserAccount user = getUser(userId);
        // Lưu ảnh TRƯỚC mọi thay đổi dữ liệu: ảnh không hợp lệ thì dừng ở đây, hồ sơ giữ nguyên
        if (hasFile(form.getAvatar())) {
            replaceAvatar(user, form.getAvatar());
        }
        user.setDisplayName(form.getDisplayName().trim());
        user.setBio(form.getBio() == null || form.getBio().isBlank() ? null : form.getBio().trim());
        // Tên hiển thị và ảnh đại diện nằm trong phiên đăng nhập, phải báo để phiên được làm mới
        eventPublisher.publishEvent(new AccountChangedEvent(userId));
    }

    /**
     * Đổi mật khẩu của chính người đang đăng nhập.
     *
     * @throws FieldValidationException gom mọi lỗi theo ô nhập: mật khẩu hiện tại sai, mật khẩu mới chưa đạt
     *                                  chính sách hoặc trùng mật khẩu cũ, xác nhận không khớp
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordForm form) {
        UserAccount user = getUser(userId);
        validatePasswordChange(form, user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        auditService.recordForCurrentUser(AuditAction.PASSWORD_CHANGED,
                AuditedEntity.of(UserAccount.class, userId), null);
    }

    private void validatePasswordChange(ChangePasswordForm form, String currentHash) {
        List<FieldViolation> violations = new ArrayList<>();
        if (!passwordEncoder.matches(form.getCurrentPassword(), currentHash)) {
            violations.add(new FieldViolation(FIELD_CURRENT_PASSWORD, MessageKeys.ERROR_PASSWORD_CURRENT_WRONG));
        }
        passwordPolicy.check(FIELD_NEW_PASSWORD, form.getNewPassword()).ifPresent(violations::add);
        if (form.getNewPassword().equals(form.getCurrentPassword())) {
            violations.add(new FieldViolation(FIELD_NEW_PASSWORD, MessageKeys.ERROR_PASSWORD_SAME_AS_CURRENT));
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            violations.add(new FieldViolation(FIELD_CONFIRM_PASSWORD, MessageKeys.ERROR_PASSWORD_CONFIRM_MISMATCH));
        }
        if (!violations.isEmpty()) {
            throw new FieldValidationException(violations);
        }
    }

    private void replaceAvatar(UserAccount user, MultipartFile avatar) {
        String previousKey = user.getAvatarPath();
        StoredFile stored = storageService.storeImage(avatar, StorageConstants.AVATAR_DIRECTORY + "/" + user.getId());
        user.setAvatarPath(stored.key());
        storageCleanup.deleteAfterCommit(previousKey);
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
