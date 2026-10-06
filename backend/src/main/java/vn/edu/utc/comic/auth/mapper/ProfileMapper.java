package vn.edu.utc.comic.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.comic.auth.dto.ProfileForm;
import vn.edu.utc.comic.auth.dto.ProfileResponse;
import vn.edu.utc.comic.user.entity.UserAccount;

/** Ánh xạ tài khoản sang dữ liệu trang hồ sơ. */
@Mapper
public interface ProfileMapper {

    /**
     * @param avatarUrl URL ảnh đại diện do service dựng qua StorageService (mapper không gọi service)
     */
    ProfileResponse toResponse(UserAccount user, String avatarUrl);

    /** Ô chọn ảnh luôn bắt đầu trống: trình duyệt không cho điền sẵn giá trị vào ô tệp. */
    @Mapping(target = "avatar", ignore = true)
    ProfileForm toForm(UserAccount user);
}
