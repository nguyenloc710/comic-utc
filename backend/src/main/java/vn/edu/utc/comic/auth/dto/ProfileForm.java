package vn.edu.utc.comic.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.comic.common.constant.UserConstants;

/** Form sửa hồ sơ cá nhân. Tên đăng nhập và email không sửa được ở đây. */
@Getter
@Setter
public class ProfileForm {

    @NotBlank(message = "{validation.display.name.required}")
    @Size(max = UserConstants.DISPLAY_NAME_MAX_LENGTH, message = "{validation.display.name.size}")
    private String displayName;

    @Size(max = UserConstants.BIO_MAX_LENGTH, message = "{validation.bio.size}")
    private String bio;

    /** Ảnh đại diện mới; bỏ trống thì giữ ảnh cũ. */
    private MultipartFile avatar;
}
