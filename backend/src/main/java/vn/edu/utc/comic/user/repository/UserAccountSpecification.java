package vn.edu.utc.comic.user.repository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.utc.comic.user.dto.UserFilterRequest;
import vn.edu.utc.comic.user.entity.UserAccount;

/** Bộ lọc danh sách tài khoản ở trang quản trị. */
public final class UserAccountSpecification {

    /** Ký tự đại diện của LIKE; phải thoát để từ khóa người dùng gõ được tìm đúng nguyên văn. */
    private static final char ESCAPE_CHARACTER = '\\';

    public static Specification<UserAccount> forFilter(UserFilterRequest filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.role() != null) {
                predicates.add(builder.equal(root.get("role"), filter.role()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = "%" + escapeLike(filter.keyword().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("username")), pattern, ESCAPE_CHARACTER),
                        builder.like(builder.lower(root.get("email")), pattern, ESCAPE_CHARACTER),
                        builder.like(builder.lower(root.get("displayName")), pattern, ESCAPE_CHARACTER)));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private UserAccountSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }
}
