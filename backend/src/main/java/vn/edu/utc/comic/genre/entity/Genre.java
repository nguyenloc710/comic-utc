package vn.edu.utc.comic.genre.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.AuditableEntity;

/** Thể loại truyện. Thể loại đang có truyện không bị xóa, chỉ tắt bằng {@code active}. */
@Getter
@Setter
@Entity
@Table(name = "genre")
public class Genre extends AuditableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /** Định danh trên URL và là tham số chatbot dùng khi gọi hàm tìm truyện. */
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    /** Được chèn vào prompt để mô hình ánh xạ lời người dùng sang thể loại. */
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
