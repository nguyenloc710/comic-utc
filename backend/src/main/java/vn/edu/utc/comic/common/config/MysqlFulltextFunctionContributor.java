package vn.edu.utc.comic.common.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Đăng ký hàm tìm kiếm toàn văn của MySQL cho Hibernate (JPQL / Criteria không có cú pháp MATCH ... AGAINST).
 *
 * <p>Hibernate tìm lớp này qua tệp {@code META-INF/services/org.hibernate.boot.model.FunctionContributor},
 * nên nó không phải bean của Spring.
 */
public class MysqlFulltextFunctionContributor implements FunctionContributor {

    /**
     * Điểm khớp của một truyện với từ khóa trên chỉ mục FULLTEXT ft_story_search; 0 nghĩa là không khớp.
     * Tham số: title, alt_title, description, từ khóa — ba cột phải đúng thứ tự đã khai báo trong chỉ mục.
     */
    public static final String STORY_FULLTEXT_SCORE = "story_fulltext_score";

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                STORY_FULLTEXT_SCORE,
                "match(?1, ?2, ?3) against (?4 in natural language mode)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.DOUBLE));
    }
}
