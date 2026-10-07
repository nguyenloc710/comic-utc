package vn.edu.utc.comic.system.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import vn.edu.utc.comic.common.audit.AuditLog;
import vn.edu.utc.comic.common.setting.Setting;
import vn.edu.utc.comic.system.dto.AuditLogResponse;
import vn.edu.utc.comic.system.dto.SettingResponse;

/** Ánh xạ tham số vận hành và nhật ký kiểm toán sang DTO cho trang quản trị. */
@Mapper
public interface SystemMapper {

    SettingResponse toResponse(Setting setting);

    List<SettingResponse> toSettingResponses(List<Setting> settings);

    AuditLogResponse toResponse(AuditLog auditLog);

    List<AuditLogResponse> toAuditLogResponses(List<AuditLog> auditLogs);
}
