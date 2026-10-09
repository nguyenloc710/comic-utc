package vn.edu.utc.comic.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.support.SqlStatementCounter;

/**
 * Chống N+1 ở các trang đông người xem nhất: số câu SQL của một trang không được tăng theo số truyện, số chương
 * hay số thể loại đang hiển thị. Test so sánh hai lần đo thay vì chốt một con số cố định, để thêm một truy vấn hợp
 * lệ cho trang không làm vỡ test, còn vòng lặp truy vấn theo từng phần tử thì vẫn bị bắt.
 */
class QueryCountIntegrationTest extends AbstractIntegrationTest {

    private static final int EXTRA_STORIES = 4;
    private static final int MANY_CHAPTERS = 6;

    @Test
    void homeAndListPages_doNotQueryOncePerStory() throws Exception {
        int homeBefore = countStatements("/");
        int listBefore = countStatements("/stories");

        for (int index = 0; index < EXTRA_STORIES; index++) {
            createStoryWithChapters(MANY_CHAPTERS);
        }

        assertThat(countStatements("/")).isLessThanOrEqualTo(homeBefore);
        assertThat(countStatements("/stories")).isLessThanOrEqualTo(listBefore);
    }

    @Test
    void storyDetail_andReader_doNotQueryOncePerChapterOrGenre() throws Exception {
        Story small = createStoryWithChapters(1);
        Story large = createStoryWithChapters(MANY_CHAPTERS);

        assertThat(countStatements("/stories/" + large.getSlug()))
                .isEqualTo(countStatements("/stories/" + small.getSlug()));
        assertThat(countStatements("/stories/" + large.getSlug() + "/chapters/1"))
                .isEqualTo(countStatements("/stories/" + small.getSlug() + "/chapters/1"));
    }

    /** Truyện tranh công khai có ba thể loại và {@code chapters} chương đã đăng, vừa cập nhật để lên trang chủ. */
    private Story createStoryWithChapters(int chapters) {
        Story story = createStory(StoryType.COMIC, created -> {
            addGenres(created, "hanh-dong", "am-thuc", "lich-su");
            created.setLastChapterAt(Instant.now());
        });
        for (int chapterNo = 1; chapterNo <= chapters; chapterNo++) {
            createChapter(story, chapterNo, ChapterStatus.PUBLISHED);
        }
        return story;
    }

    /** Gọi trước một lần để cache (thể loại, tham số) ấm rồi mới đếm, chỉ còn lại truy vấn thật của trang. */
    private int countStatements(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk());
        SqlStatementCounter.reset();
        mockMvc.perform(get(url)).andExpect(status().isOk());
        return SqlStatementCounter.count();
    }
}
