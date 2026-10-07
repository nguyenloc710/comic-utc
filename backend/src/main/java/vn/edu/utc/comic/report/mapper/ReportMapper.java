package vn.edu.utc.comic.report.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.comic.report.dto.ReportResponse;
import vn.edu.utc.comic.report.dto.ReportTarget;
import vn.edu.utc.comic.report.entity.Report;

/** Ánh xạ báo cáo vi phạm. Phải gọi trong transaction vì đọc quan hệ lazy tới người báo cáo và người xử lý. */
@Mapper
public interface ReportMapper {

    /**
     * @param target nội dung bị báo cáo do service tra ra (báo cáo không có khóa ngoại tới nội dung)
     */
    @Mapping(target = "id", source = "report.id")
    @Mapping(target = "targetType", source = "report.targetType")
    @Mapping(target = "targetId", source = "report.targetId")
    @Mapping(target = "targetLabel", source = "target.label")
    @Mapping(target = "targetLink", source = "target.link")
    @Mapping(target = "targetHidden", source = "target.hidden")
    @Mapping(target = "reporterUsername", source = "report.reporter.username")
    @Mapping(target = "handlerUsername", source = "report.handledBy.username")
    @Mapping(target = "reason", source = "report.reason")
    @Mapping(target = "detail", source = "report.detail")
    @Mapping(target = "status", source = "report.status")
    @Mapping(target = "handledAt", source = "report.handledAt")
    @Mapping(target = "resolutionNote", source = "report.resolutionNote")
    @Mapping(target = "createdAt", source = "report.createdAt")
    ReportResponse toResponse(Report report, ReportTarget target);
}
