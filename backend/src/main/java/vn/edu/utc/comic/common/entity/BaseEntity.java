package vn.edu.utc.comic.common.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

/**
 * Lớp cha cho mọi thực thể có khóa chính tự tăng.
 *
 * <p>equals/hashCode theo khuyến nghị của Hibernate: so sánh theo id và lớp thực,
 * hashCode trả về hằng số theo lớp để đối tượng chưa lưu vẫn hoạt động đúng trong HashSet.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        // Với proxy của Hibernate phải lấy lớp thực, không dùng getClass() trực tiếp
        if (!resolveEntityClass(this).equals(resolveEntityClass(other))) {
            return false;
        }
        BaseEntity otherEntity = (BaseEntity) other;
        return id != null && Objects.equals(id, otherEntity.getId());
    }

    @Override
    public final int hashCode() {
        return resolveEntityClass(this).hashCode();
    }

    private static Class<?> resolveEntityClass(Object entity) {
        return entity instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : entity.getClass();
    }
}
