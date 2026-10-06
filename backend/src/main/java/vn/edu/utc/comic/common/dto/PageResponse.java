package vn.edu.utc.comic.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Kết quả phân trang gọn nhẹ dùng cho cả template lẫn JSON, thay cho việc trả thẳng Page của Spring Data.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    /** Chuyển một trang thực thể sang trang DTO bằng hàm ánh xạ cho trước. */
    public static <E, D> PageResponse<D> of(Page<E> page, Function<List<E>, List<D>> mapper) {
        return new PageResponse<>(
                mapper.apply(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }

    /** Chỉ số trang hiển thị (bắt đầu từ 1) để template không phải cộng thêm. */
    public int displayPage() {
        return page + 1;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
