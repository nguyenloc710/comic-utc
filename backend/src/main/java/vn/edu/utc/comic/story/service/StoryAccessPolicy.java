package vn.edu.utc.comic.story.service;

import org.springframework.stereotype.Component;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryVisibility;

/**
 * Ai được xem truyện và chương nào. Đây là lớp kiểm tra quyền theo DỮ LIỆU (truyện này của ai),
 * bổ sung cho lớp phân quyền theo URL ở SecurityConfig.
 */
@Component
public class StoryAccessPolicy {

    /**
     * Truyện công khai thì ai cũng xem được. Truyện nháp hoặc đang bị ẩn chỉ tác giả của nó và quản trị viên
     * xem trước được. Truyện đã xóa thì không ai xem được.
     */
    public boolean canView(Story story, Viewer viewer) {
        if (story.getDeletedAt() != null) {
            return false;
        }
        return story.getVisibility() == StoryVisibility.PUBLISHED || isOwnerOrAdmin(story, viewer);
    }

    /** Phải xem được truyện trước đã; chương chưa đăng hoặc đang bị ẩn chỉ tác giả và quản trị viên đọc được. */
    public boolean canRead(Chapter chapter, Story story, Viewer viewer) {
        if (!canView(story, viewer)) {
            return false;
        }
        return chapter.getStatus().isPubliclyReadable() || isOwnerOrAdmin(story, viewer);
    }

    /** Người đọc bất kỳ (kể cả khách vãng lai) có đọc được chương này không. */
    public boolean isPubliclyReadable(Chapter chapter, Story story) {
        return canRead(chapter, story, Viewer.anonymous());
    }

    /**
     * Ai được SỬA truyện trong khu vực tác giả: chỉ chính tác giả, và chỉ khi truyện chưa bị xóa. Quản trị viên
     * không sửa truyện của tác giả; họ kiểm duyệt (ẩn / hiện) qua màn quản trị.
     */
    public boolean canManage(Story story, Long userId) {
        return story.getDeletedAt() == null && story.getAuthor().getId().equals(userId);
    }
    private static boolean isOwnerOrAdmin(Story story, Viewer viewer) {
        return viewer.isAdmin() || viewer.isUser(story.getAuthor().getId());
    }
}
