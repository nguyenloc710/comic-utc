package vn.edu.utc.comic.chapter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.utc.comic.chapter.enums.ChapterFormAction;
import vn.edu.utc.comic.common.constant.ChapterConstants;

/**
 * Form soạn chương của tác giả, dùng chung cho truyện tranh và truyện chữ.
 * Ảnh của chương truyện tranh không đi qua form này: chúng được tải lên từng tấm qua API riêng.
 */
@Getter
@Setter
public class ChapterForm {

    /** Số thứ tự chương, duy nhất trong truyện; khóa lại sau khi chương đã đăng. */
    @NotNull(message = "{validation.chapter.no.range}")
    @Min(value = ChapterConstants.CHAPTER_NO_MIN, message = "{validation.chapter.no.range}")
    @Max(value = ChapterConstants.CHAPTER_NO_MAX, message = "{validation.chapter.no.range}")
    private Integer chapterNo;

    @Size(max = ChapterConstants.TITLE_MAX_LENGTH, message = "{validation.chapter.title.size}")
    private String title;

    /** HTML từ trình soạn thảo, chỉ dùng cho truyện chữ; service làm sạch trước khi lưu. */
    @Size(max = ChapterConstants.CONTENT_MAX_LENGTH, message = "{validation.chapter.content.size}")
    private String contentHtml;

    /** Giờ hẹn đăng theo giờ Việt Nam (đúng như tác giả chọn trên form); service đổi sang UTC khi lưu. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledAt;

    private ChapterFormAction action = ChapterFormAction.SAVE;
}
