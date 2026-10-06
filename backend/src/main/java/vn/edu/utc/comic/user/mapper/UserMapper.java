package vn.edu.utc.comic.user.mapper;

import java.time.Instant;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.comic.user.dto.UserSummaryResponse;
import vn.edu.utc.comic.user.entity.UserAccount;

/** Ánh xạ tài khoản sang DTO cho trang quản trị. Mật khẩu băm không bao giờ đi ra ngoài. */
@Mapper
public interface UserMapper {

    /**
     * @param now thời điểm hiện tại, để xác định tài khoản có đang bị khóa tạm hay không
     */
    @Mapping(target = "temporarilyLocked", expression = "java(user.isLockedAt(now))")
    UserSummaryResponse toSummary(UserAccount user, @Context Instant now);

    List<UserSummaryResponse> toSummaries(List<UserAccount> users, @Context Instant now);
}
