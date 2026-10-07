package vn.edu.utc.comic.author.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.author.enums.IntendedStoryType;
import vn.edu.utc.comic.common.constant.AuthorConstants;

/** Form độc giả gửi yêu cầu đăng ký làm tác giả. */
@Getter
@Setter
public class AuthorRequestForm {

    /** Tên hiện trên trang truyện thay cho tên tài khoản; duy nhất toàn hệ thống. */
    @NotBlank(message = "{validation.author.pen.name.required}")
    @Size(min = AuthorConstants.PEN_NAME_MIN_LENGTH, max = AuthorConstants.PEN_NAME_MAX_LENGTH,
            message = "{validation.author.pen.name.size}")
    private String penName;

    @NotBlank(message = "{validation.author.introduction.required}")
    @Size(min = AuthorConstants.INTRODUCTION_MIN_LENGTH, max = AuthorConstants.INTRODUCTION_MAX_LENGTH,
            message = "{validation.author.introduction.size}")
    private String introduction;

    @NotNull(message = "{validation.author.intended.type.required}")
    private IntendedStoryType intendedType;
}
