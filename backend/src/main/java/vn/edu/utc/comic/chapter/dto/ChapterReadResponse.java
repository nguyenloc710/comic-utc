package vn.edu.utc.comic.chapter.dto;

import java.time.Instant;
import java.util.List;
import vn.edu.utc.comic.story.enums.StoryType;

/**
 * Mọi thứ trang đọc cần cho một chương.
 *
 * @param authorId           tác giả của truyện, để không tính lượt xem khi tác giả tự đọc
 * @param publiclyReadable   {@code false} nghĩa là người xem đang xem trước chương chưa công khai
 * @param pages              các trang ảnh (truyện tranh); rỗng với truyện chữ
 * @param contentHtml        nội dung HTML đã làm sạch (truyện chữ); {@code null} với truyện tranh
 * @param previousChapterNo  số của chương đã đăng liền trước; {@code null} nếu đây là chương đầu
 * @param nextChapterNo      số của chương đã đăng liền sau; {@code null} nếu đây là chương mới nhất
 */
public record ChapterReadResponse(
        Long storyId,
        String storySlug,
        String storyTitle,
        StoryType storyType,
        Long authorId,
        Long chapterId,
        int chapterNo,
        String title,
        Instant publishedAt,
        boolean publiclyReadable,
        List<ChapterPageResponse> pages,
        String contentHtml,
        Integer previousChapterNo,
        Integer nextChapterNo) {

    /** Phần thân của chương: ảnh hoặc văn bản tùy loại truyện. */
    public record Body(List<ChapterPageResponse> pages, String contentHtml) {
    }

    /** Chương đã đăng liền trước và liền sau, để dựng nút điều hướng. */
    public record Neighbors(Integer previousChapterNo, Integer nextChapterNo) {
    }
}
