package vn.edu.utc.comic.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Lớp cha cho thực thể cần ghi nhận ai tạo, ai sửa và khi nào.
 * Các cột này do Spring Data JPA Auditing tự điền, không gán tay trong service.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditableEntity extends CreatedAtEntity {

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Id tài khoản đã tạo bản ghi; rỗng nếu do hệ thống (job) tạo hoặc người dùng tự đăng ký. */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}
