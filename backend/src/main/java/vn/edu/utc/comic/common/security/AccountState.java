package vn.edu.utc.comic.common.security;

import java.io.Serializable;
import vn.edu.utc.comic.user.enums.Role;
import vn.edu.utc.comic.user.enums.UserStatus;

/**
 * Phần thông tin tài khoản có thể thay đổi trong lúc người dùng đang đăng nhập: được duyệt làm tác giả,
 * bị khóa, đổi tên hiển thị hay ảnh đại diện.
 *
 * <p>Phiên đăng nhập giữ một bản; AuthorityRefreshFilter so bản đó với giá trị hiện tại trong cơ sở dữ liệu
 * ở mỗi request và làm mới phiên khi lệch.
 */
public record AccountState(Role role, UserStatus status, String displayName, String avatarPath)
        implements Serializable {

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
