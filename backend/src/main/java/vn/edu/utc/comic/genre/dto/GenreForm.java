package vn.edu.utc.comic.genre.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.constant.GenreConstants;

/** Form tạo / sửa thể loại. Slug không có ở đây: nó được sinh từ tên lúc tạo và không đổi về sau. */
@Getter
@Setter
public class GenreForm {

    @NotBlank(message = "{validation.genre.name.required}")
    @Size(max = GenreConstants.NAME_MAX_LENGTH, message = "{validation.genre.name.size}")
    private String name;

    @Size(max = GenreConstants.DESCRIPTION_MAX_LENGTH, message = "{validation.genre.description.size}")
    private String description;

    @NotNull(message = "{validation.genre.sort.order.range}")
    @Min(value = 0, message = "{validation.genre.sort.order.range}")
    @Max(value = GenreConstants.SORT_ORDER_MAX, message = "{validation.genre.sort.order.range}")
    private Integer sortOrder = 0;

    /** Tắt thì thể loại không còn xuất hiện ở bộ lọc và form đăng truyện, truyện đã gắn vẫn giữ nguyên. */
    private boolean active = true;
}
