package vn.edu.utc.comic.interaction.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.comic.common.storage.MediaUrlMapper;
import java.util.List;
import vn.edu.utc.comic.interaction.dto.AdminCommentResponse;
import vn.edu.utc.comic.interaction.dto.CommentResponse;
import vn.edu.utc.comic.interaction.entity.Comment;
import vn.edu.utc.comic.interaction.enums.CommentStatus;

/** Ánh xạ bình luận sang DTO trả cho giao diện. */
@Mapper(uses = MediaUrlMapper.class, imports = CommentStatus.class)
public interface CommentMapper {

    /**
     * @param viewerId người đang xem, để đánh dấu bình luận của chính họ; {@code null} với khách vãng lai
     * @param replies  các câu trả lời đã ánh xạ sẵn
     */
    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "parentId", source = "comment.parent.id")
    @Mapping(target = "authorName", source = "comment.user.displayName")
    @Mapping(target = "authorAvatarUrl", source = "comment.user.avatarPath", qualifiedByName = MediaUrlMapper.MEDIA_URL)
    // Nội dung của bình luận bị ẩn hoặc đã xóa KHÔNG được rời máy chủ: giấu bằng giao diện là chưa đủ
    @Mapping(target = "content",
            expression = "java(comment.getStatus() == CommentStatus.VISIBLE ? comment.getContent() : null)")
    @Mapping(target = "status", source = "comment.status")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "mine", expression = "java(viewerId != null && viewerId.equals(comment.getUser().getId()))")
    @Mapping(target = "replies", source = "replies")
    CommentResponse toResponse(Comment comment, Long viewerId, List<CommentResponse> replies);

    /** Cho màn kiểm duyệt: quản trị viên đọc được cả nội dung đã bị ẩn. */
    @Mapping(target = "authorId", source = "user.id")
    @Mapping(target = "authorUsername", source = "user.username")
    @Mapping(target = "storyTitle", source = "story.title")
    @Mapping(target = "storySlug", source = "story.slug")
    @Mapping(target = "chapterNo", source = "chapter.chapterNo")
    AdminCommentResponse toAdminResponse(Comment comment);

    List<AdminCommentResponse> toAdminResponses(List<Comment> comments);
}
