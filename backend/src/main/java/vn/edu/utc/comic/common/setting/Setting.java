package vn.edu.utc.comic.common.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Một tham số vận hành: giá trị nghiệp vụ có thể đổi mà không phải build lại ứng dụng.
 */
@Getter
@Setter
@Entity
@Table(name = "setting")
public class Setting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_read_only", nullable = false)
    private boolean readOnly;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
