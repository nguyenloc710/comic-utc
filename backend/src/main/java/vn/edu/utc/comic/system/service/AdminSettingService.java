package vn.edu.utc.comic.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.audit.AuditAction;
import vn.edu.utc.comic.common.audit.AuditService;
import vn.edu.utc.comic.common.audit.AuditService.AuditedEntity;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.setting.Setting;
import vn.edu.utc.comic.common.setting.SettingRepository;
import vn.edu.utc.comic.common.setting.SettingService;
import vn.edu.utc.comic.common.setting.SettingValueType;
import vn.edu.utc.comic.system.dto.SettingResponse;
import vn.edu.utc.comic.system.mapper.SystemMapper;

/**
 * Quản trị viên xem và sửa tham số vận hành. Giá trị mới được kiểm tra theo kiểu khai báo của tham số,
 * và cache tham số được xóa ngay sau khi lưu để mọi request kế tiếp dùng giá trị mới.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettingService {

    private static final String AUDIT_OLD_VALUE = "oldValue";
    private static final String AUDIT_NEW_VALUE = "newValue";
    private static final String SORT_GROUP = "groupName";
    private static final String SORT_KEY = "settingKey";

    private final SettingRepository settingRepository;
    private final SettingService settingService;
    private final SystemMapper systemMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** Mọi tham số, gom theo nhóm (thứ tự nhóm rồi khóa). */
    @Transactional(readOnly = true)
    public Map<String, List<SettingResponse>> findAllGrouped() {
        List<Setting> settings = settingRepository.findAll(Sort.by(SORT_GROUP, SORT_KEY));
        return systemMapper.toSettingResponses(settings).stream()
                .collect(Collectors.groupingBy(SettingResponse::groupName, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Đổi giá trị một tham số.
     *
     * @throws ApiException SETTING_NOT_FOUND; SETTING_READ_ONLY; SETTING_VALUE_INVALID nếu giá trị không đúng kiểu
     *                      (số nguyên, số thập phân, true/false, JSON hợp lệ)
     */
    @Transactional
    public void updateSetting(String key, String rawValue, Long adminId) {
        Setting setting = settingRepository.findById(key)
                .orElseThrow(() -> new ApiException(ErrorCode.SETTING_NOT_FOUND, key));
        if (setting.isReadOnly()) {
            throw new ApiException(ErrorCode.SETTING_READ_ONLY, key);
        }
        String value = normalize(setting, rawValue == null ? "" : rawValue.trim());
        String oldValue = setting.getSettingValue();
        setting.setSettingValue(value);
        setting.setUpdatedAt(clock.instant());
        setting.setUpdatedBy(adminId);
        // Xóa cache ngay trong transaction: lỡ có request đọc lại giữa chừng thì cũng chỉ cache lại giá trị cũ
        // thêm một lúc, không sai dữ liệu; xóa sau commit mới là an toàn tuyệt đối nhưng SettingService không mở
        // transaction ghi nào nên giữ cách đơn giản
        settingService.clearCache();
        auditService.recordForCurrentUser(AuditAction.SETTING_CHANGED, AuditedEntity.of(Setting.class, key),
                Map.of(AUDIT_OLD_VALUE, oldValue, AUDIT_NEW_VALUE, value));
        log.info("Đổi tham số {} từ {} thành {}", key, oldValue, value);
    }

    /** Kiểm tra theo kiểu và đưa về dạng chuẩn ("True" → "true", "007" → "7"). */
    private String normalize(Setting setting, String value) {
        SettingValueType type = SettingValueType.valueOf(setting.getValueType());
        try {
            return switch (type) {
                case INTEGER -> String.valueOf(Integer.parseInt(value));
                case DECIMAL -> new BigDecimal(value).toPlainString();
                case BOOLEAN -> normalizeBoolean(value);
                case JSON -> objectMapper.readTree(value).toString();
                case STRING -> value;
            };
        } catch (NumberFormatException | JsonProcessingException exception) {
            throw new ApiException(ErrorCode.SETTING_VALUE_INVALID, exception, setting.getSettingKey(), type);
        }
    }

    private static String normalizeBoolean(String value) {
        String lowerCased = value.toLowerCase(Locale.ROOT);
        if (!Boolean.TRUE.toString().equals(lowerCased) && !Boolean.FALSE.toString().equals(lowerCased)) {
            throw new NumberFormatException(value);
        }
        return lowerCased;
    }
}
