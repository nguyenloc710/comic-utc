package vn.edu.utc.comic.story.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.utc.comic.common.config.MysqlFulltextFunctionContributor;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.story.dto.StoryFilterRequest;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StorySort;
import vn.edu.utc.comic.story.enums.StoryVisibility;

/**
 * Điều kiện truy vấn truyện.
 *
 * <p>{@link #publiclyVisible()} là định nghĩa DUY NHẤT của "truyện mà người đọc được thấy"; mọi truy vấn
 * phía người đọc và mọi hàm của chatbot đều phải ghép nó vào.
 */
public final class StorySpecification {

    private static final char LIKE_ESCAPE = '\\';

    /** Truyện đang công khai và chưa bị xóa mềm. */
    public static Specification<Story> publiclyVisible() {
        return (root, query, builder) -> builder.and(
                builder.equal(root.get("visibility"), StoryVisibility.PUBLISHED),
                builder.isNull(root.get("deletedAt")));
    }

    /** Truyện có ít nhất chừng này lượt đánh giá (điều kiện vào bảng xếp hạng theo điểm). */
    public static Specification<Story> ratedAtLeast(int minRatingCount) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("ratingCount"), minRatingCount);
    }

    /** Các tiêu chí lọc của người dùng, kèm thứ tự sắp xếp tương ứng. */
    public static Specification<Story> matching(StoryFilterRequest filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.hasKeyword()) {
                predicates.add(keywordPredicate(root, builder, filter.keyword()));
            }
            // Mỗi thể loại bắt buộc là một EXISTS riêng: truyện phải có ĐỦ mọi thể loại đã chọn
            filter.genres().forEach(slug -> predicates.add(builder.exists(genreSubquery(root, query, builder, List.of(slug)))));
            if (!filter.excludeGenres().isEmpty()) {
                predicates.add(builder.not(builder.exists(genreSubquery(root, query, builder, filter.excludeGenres()))));
            }
            if (filter.type() != null) {
                predicates.add(builder.equal(root.get("type"), filter.type()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            if (filter.minChapters() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("chapterCount"), filter.minChapters()));
            }
            if (filter.maxChapters() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("chapterCount"), filter.maxChapters()));
            }
            // Truy vấn đếm tổng số dòng của Spring Data cũng đi qua đây nhưng không cần sắp xếp
            if (!Long.class.equals(query.getResultType())) {
                query.orderBy(buildOrder(root, builder, filter));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Khớp toàn văn trên tên / tên khác / mô tả, cộng thêm khớp một phần tên để gõ dở một từ vẫn ra kết quả.
     * Nhờ collation utf8mb4_unicode_ci, gõ không dấu ("tu tien") vẫn tìm được "Tu tiên". Ngoại lệ là chữ "đ":
     * collation coi nó là một chữ cái khác "d", nên "do thi" không khớp "Đô thị".
     */
    private static Predicate keywordPredicate(Root<Story> root, CriteriaBuilder builder, String keyword) {
        String pattern = "%" + escapeLike(keyword) + "%";
        return builder.or(
                builder.greaterThan(fulltextScore(root, builder, keyword), 0.0),
                builder.like(root.get("title"), pattern, LIKE_ESCAPE),
                builder.like(root.get("altTitle"), pattern, LIKE_ESCAPE));
    }

    private static Expression<Double> fulltextScore(Root<Story> root, CriteriaBuilder builder, String keyword) {
        // value(...) tạo tham số ràng buộc thay vì chèn từ khóa thành hằng trong câu SQL
        Expression<String> keywordParameter = ((HibernateCriteriaBuilder) builder).value(keyword);
        return builder.function(MysqlFulltextFunctionContributor.STORY_FULLTEXT_SCORE, Double.class,
                root.get("title"), root.get("altTitle"), root.get("description"), keywordParameter);
    }

    /** Truy vấn con tương quan: truyện đang xét có thuộc một trong các thể loại này không. */
    private static Subquery<Long> genreSubquery(Root<Story> root, CriteriaQuery<?> query, CriteriaBuilder builder,
                                                List<String> genreSlugs) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Story> story = subquery.from(Story.class);
        Join<Story, Genre> genre = story.join("genres");
        return subquery.select(story.get("id")).where(
                builder.equal(story.get("id"), root.get("id")),
                genre.get("slug").in(genreSlugs));
    }

    private static List<Order> buildOrder(Root<Story> root, CriteriaBuilder builder, StoryFilterRequest filter) {
        List<Order> orders = new ArrayList<>();
        if (filter.sort() == null && filter.hasKeyword()) {
            orders.add(builder.desc(fulltextScore(root, builder, filter.keyword())));
            orders.add(builder.desc(root.get("viewCount")));
        } else {
            orders.add(builder.desc(sortExpression(root, builder, filter.sort())));
        }
        // Tiêu chí phụ cố định để phân trang không lặp hay sót truyện khi nhiều truyện bằng điểm nhau
        orders.add(builder.desc(root.get("id")));
        return orders;
    }

    private static Expression<?> sortExpression(Root<Story> root, CriteriaBuilder builder, StorySort sort) {
        if (sort == null) {
            return root.get("lastChapterAt");
        }
        return switch (sort) {
            case UPDATED -> root.get("lastChapterAt");
            case NEWEST -> root.get("publishedAt");
            case VIEWS -> root.get("viewCount");
            case FOLLOWS -> root.get("followCount");
            // nullif để truyện chưa có lượt đánh giá nào không gây chia cho 0; MySQL xếp NULL cuối khi giảm dần
            case RATING -> builder.quot(root.<Integer>get("ratingSum"),
                    builder.nullif(root.<Integer>get("ratingCount"), 0));
        };
    }

    private static String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private StorySpecification() {
        throw new UnsupportedOperationException("Utility class");
    }
}
