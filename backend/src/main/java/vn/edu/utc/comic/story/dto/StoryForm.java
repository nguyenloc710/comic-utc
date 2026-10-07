package vn.edu.utc.comic.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.common.constant.StoryConstants;
import vn.edu.utc.comic.story.enums.StoryStatus;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Form tạo / sửa truyện của tác giả. Slug không có ở đây: nó được sinh từ tên lúc tạo và không đổi về sau.
 * Trạng thái hiển thị (nháp / công khai / bị ẩn) cũng không có: nó đổi qua thao tác riêng.
 */
@Getter
@Setter
public class StoryForm {

    @NotBlank(message = "{validation.story.title.required}")
    @Size(max = StoryConstants.TITLE_MAX_LENGTH, message = "{validation.story.title.size}")
    private String title;

    @Size(max = StoryConstants.TITLE_MAX_LENGTH, message = "{validation.story.alt.title.size}")
    private String altTitle;

    @NotBlank(message = "{validation.story.description.required}")
    @Size(max = StoryConstants.DESCRIPTION_MAX_LENGTH, message = "{validation.story.description.size}")
    private String description;

    /** Chọn lúc tạo, khóa lại khi truyện đã có chương (trang đọc của hai loại khác hẳn nhau). */
    @NotNull(message = "{validation.story.type.required}")
    private StoryType type;

    /** Tiến độ sáng tác do tác giả tự đặt. */
    @NotNull(message = "{validation.story.status.required}")
    private StoryStatus status = StoryStatus.ONGOING;

    @Size(max = StoryConstants.GENRES_MAX, message = "{validation.story.genres.size}")
    private List<Long> genreIds = new ArrayList<>();

    /** Ảnh bìa; bỏ trống khi sửa nghĩa là giữ bìa hiện tại. */
    private MultipartFile cover;
}
