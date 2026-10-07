package vn.edu.utc.comic.author.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.comic.author.dto.AuthorRequestResponse;
import vn.edu.utc.comic.author.entity.AuthorRequest;

/** Ánh xạ yêu cầu đăng ký tác giả. Phải gọi trong transaction vì đọc quan hệ lazy tới người gửi. */
@Mapper
public interface AuthorRequestMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "displayName", source = "user.displayName")
    @Mapping(target = "email", source = "user.email")
    AuthorRequestResponse toResponse(AuthorRequest request);

    List<AuthorRequestResponse> toResponses(List<AuthorRequest> requests);
}
