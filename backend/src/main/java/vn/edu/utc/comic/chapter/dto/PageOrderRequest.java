package vn.edu.utc.comic.chapter.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Thứ tự mới của các trang trong một chương truyện tranh.
 *
 * @param pageIds id của MỌI trang trong chương, theo thứ tự mới từ trang 1
 */
public record PageOrderRequest(
        @NotEmpty(message = "{validation.chapter.page.order.required}")
        List<@NotNull(message = "{validation.chapter.page.order.required}") Long> pageIds) {
}
