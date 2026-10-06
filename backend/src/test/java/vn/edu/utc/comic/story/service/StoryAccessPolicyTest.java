package vn.edu.utc.comic.story.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.user.entity.UserAccount;
import vn.edu.utc.comic.user.enums.Role;

/** Ma trận "ai được xem gì" của truyện và chương. */
class StoryAccessPolicyTest {

    private static final long AUTHOR_ID = 10L;
    private static final long OTHER_USER_ID = 20L;

    private final StoryAccessPolicy policy = new StoryAccessPolicy();

    @ParameterizedTest(name = "truyện {0}, đã xóa={1}: khách={2}, độc giả khác={3}, tác giả={4}, admin={5}")
    @CsvSource({
            "PUBLISHED, false, true,  true,  true,  true",
            "DRAFT,     false, false, false, true,  true",
            "HIDDEN,    false, false, false, true,  true",
            "PUBLISHED, true,  false, false, false, false",
            "DRAFT,     true,  false, false, false, false",
    })
    void canView(StoryVisibility visibility, boolean deleted, boolean guest, boolean otherReader, boolean author,
                 boolean admin) {
        Story story = story(visibility, deleted);

        assertThat(policy.canView(story, Viewer.anonymous())).isEqualTo(guest);
        assertThat(policy.canView(story, new Viewer(OTHER_USER_ID, Role.AUTHOR))).isEqualTo(otherReader);
        assertThat(policy.canView(story, new Viewer(AUTHOR_ID, Role.AUTHOR))).isEqualTo(author);
        assertThat(policy.canView(story, new Viewer(OTHER_USER_ID, Role.ADMIN))).isEqualTo(admin);
    }

    @ParameterizedTest(name = "truyện {0}, chương {1}: khách={2}, tác giả={3}, admin={4}")
    @CsvSource({
            "PUBLISHED, PUBLISHED, true,  true, true",
            "PUBLISHED, DRAFT,     false, true, true",
            "PUBLISHED, SCHEDULED, false, true, true",
            "PUBLISHED, HIDDEN,    false, true, true",
            // Chương đã đăng của một truyện chưa công khai vẫn không lộ ra ngoài
            "DRAFT,     PUBLISHED, false, true, true",
            "HIDDEN,    PUBLISHED, false, true, true",
    })
    void canRead(StoryVisibility visibility, ChapterStatus status, boolean guest, boolean author, boolean admin) {
        Story story = story(visibility, false);
        Chapter chapter = new Chapter();
        chapter.setStatus(status);

        assertThat(policy.canRead(chapter, story, Viewer.anonymous())).isEqualTo(guest);
        assertThat(policy.isPubliclyReadable(chapter, story)).isEqualTo(guest);
        assertThat(policy.canRead(chapter, story, new Viewer(OTHER_USER_ID, Role.USER))).isEqualTo(guest);
        assertThat(policy.canRead(chapter, story, new Viewer(AUTHOR_ID, Role.AUTHOR))).isEqualTo(author);
        assertThat(policy.canRead(chapter, story, new Viewer(OTHER_USER_ID, Role.ADMIN))).isEqualTo(admin);
    }

    private static Story story(StoryVisibility visibility, boolean deleted) {
        UserAccount author = new UserAccount();
        author.setId(AUTHOR_ID);
        Story story = new Story();
        story.setAuthor(author);
        story.setVisibility(visibility);
        story.setDeletedAt(deleted ? Instant.now() : null);
        return story;
    }
}
