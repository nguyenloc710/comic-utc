package vn.edu.utc.comic.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.chapter.enums.ChapterStatus;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.interaction.entity.Comment;
import vn.edu.utc.comic.interaction.enums.CommentStatus;
import vn.edu.utc.comic.interaction.repository.CommentRepository;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;
import vn.edu.utc.comic.support.AbstractIntegrationTest;
import vn.edu.utc.comic.user.enums.Role;

/**
 * Bình luận. Mỗi bình luận trong test do một tài khoản khác nhau viết, vì cùng một người phải chờ giữa hai lần gửi.
 */
class CommentApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void comment_isStoredAsPlainText_andCountedOnTheStory() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        String script = "  <script>alert('xss')</script> hay quá  ";

        addComment(principalOf(Role.USER), story, null, null, script)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value(script.trim()))
                .andExpect(jsonPath("$.data.mine").value(true))
                .andExpect(jsonPath("$.data.parentId").doesNotExist());

        assertThat(storyCounter(story, "comment_count")).isEqualTo(1);
        // Khách vãng lai cũng xem được, và bình luận không phải "của mình" với họ
        listComments(story, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value(script.trim()))
                .andExpect(jsonPath("$.data.content[0].mine").value(false));
    }

    @Test
    void writingComments_requiresLogin() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);

        mockMvc.perform(post("/api/comments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(story, null, null, "Khách không được viết")))
                .andExpect(status().isUnauthorized());
        assertThat(storyCounter(story, "comment_count")).isZero();
    }

    @Test
    void reply_toAReply_isAttachedToTheThreadRoot() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        long rootId = commentId(addComment(principalOf(Role.USER), story, null, null, "Bình luận gốc"));
        long replyId = commentId(addComment(principalOf(Role.USER), story, null, rootId, "Trả lời thứ nhất"));

        addComment(principalOf(Role.USER), story, null, replyId, "Trả lời một câu trả lời")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parentId").value(rootId));

        listComments(story, null)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].replies.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].replies[0].content").value("Trả lời thứ nhất"));
        assertThat(storyCounter(story, "comment_count")).isEqualTo(3);
    }

    @Test
    void sameUser_mustWaitBetweenTwoComments() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        AppUserPrincipal reader = principalOf(Role.USER);
        addComment(reader, story, null, null, "Bình luận đầu").andExpect(status().isCreated());

        addComment(reader, story, null, null, "Bình luận ngay sau đó")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_TOO_FAST"));

        assertThat(storyCounter(story, "comment_count")).isEqualTo(1);
    }

    @Test
    void chapterComments_areSeparateFromStoryComments() throws Exception {
        Story story = createPublishedStory(StoryType.COMIC);
        Chapter chapter = createChapter(story, 1, ChapterStatus.PUBLISHED);
        addComment(principalOf(Role.USER), story, null, null, "Ở trang truyện");
        addComment(principalOf(Role.USER), story, chapter.getId(), null, "Ở trang chương");

        listComments(story, null).andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].content").value("Ở trang truyện"));
        listComments(story, chapter.getId()).andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].content").value("Ở trang chương"));
    }

    @Test
    void comment_isRejected_whenTargetIsInvalid_andNothingIsCounted() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        Story otherStory = createPublishedStory(StoryType.NOVEL);
        Story draftStory = createStory(StoryType.NOVEL, created -> created.setVisibility(StoryVisibility.DRAFT));
        Chapter draftChapter = createChapter(story, 1, ChapterStatus.DRAFT);
        Chapter foreignChapter = createChapter(otherStory, 1, ChapterStatus.PUBLISHED);
        long foreignCommentId = commentId(addComment(principalOf(Role.USER), otherStory, null, null, "Ở truyện khác"));

        addComment(principalOf(Role.USER), draftStory, null, null, "Truyện nháp")
                .andExpect(status().isNotFound());
        addComment(principalOf(Role.USER), story, draftChapter.getId(), null, "Chương nháp")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("CHAPTER_NOT_FOUND"));
        addComment(principalOf(Role.USER), story, foreignChapter.getId(), null, "Chương của truyện khác")
                .andExpect(status().isNotFound());
        addComment(principalOf(Role.USER), story, null, foreignCommentId, "Trả lời bình luận của truyện khác")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("COMMENT_PARENT_INVALID"));
        addComment(principalOf(Role.USER), story, null, null, "   ")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("content"));
        addComment(principalOf(Role.USER), story, null, null, "x".repeat(1001))
                .andExpect(status().isBadRequest());

        assertThat(storyCounter(story, "comment_count")).isZero();
    }

    @Test
    void author_canDeleteOwnComment_whichKeepsItsPlaceButLosesItsContent() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        AppUserPrincipal writer = principalOf(Role.USER);
        long commentId = commentId(addComment(writer, story, null, null, "Sẽ bị xóa"));

        mockMvc.perform(delete("/api/comments/{id}", commentId).with(user(principalOf(Role.USER))).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
        assertThat(storyCounter(story, "comment_count")).isEqualTo(1);

        mockMvc.perform(delete("/api/comments/{id}", commentId).with(user(writer)).with(csrf()))
                .andExpect(status().isOk());
        // Xóa lần nữa không được trừ bộ đếm lần hai
        mockMvc.perform(delete("/api/comments/{id}", commentId).with(user(writer)).with(csrf()))
                .andExpect(status().isOk());

        assertThat(storyCounter(story, "comment_count")).isZero();
        listComments(story, null)
                .andExpect(jsonPath("$.data.content[0].status").value("DELETED"))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());
    }

    @Test
    void hiddenComment_neverLeavesTheServer() throws Exception {
        Story story = createPublishedStory(StoryType.NOVEL);
        long commentId = commentId(addComment(principalOf(Role.USER), story, null, null, "Nội dung vi phạm"));
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        comment.setStatus(CommentStatus.HIDDEN);
        commentRepository.saveAndFlush(comment);

        listComments(story, null)
                .andExpect(jsonPath("$.data.content[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());
    }

    @Test
    void comments_ofNonPublicStory_are404() throws Exception {
        Story draft = createStory(StoryType.NOVEL, story -> story.setVisibility(StoryVisibility.DRAFT));

        listComments(draft, null).andExpect(status().isNotFound());
    }

    private ResultActions addComment(AppUserPrincipal principal, Story story, Long chapterId, Long parentId,
                                     String content) throws Exception {
        return mockMvc.perform(post("/api/comments").with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(story, chapterId, parentId, content)));
    }

    private ResultActions listComments(Story story, Long chapterId) throws Exception {
        var request = get("/api/comments").param("storyId", String.valueOf(story.getId()));
        return mockMvc.perform(chapterId == null ? request : request.param("chapterId", String.valueOf(chapterId)));
    }

    private String body(Story story, Long chapterId, Long parentId, String content) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("storyId", story.getId());
        body.put("chapterId", chapterId);
        body.put("parentId", parentId);
        body.put("content", content);
        return objectMapper.writeValueAsString(body);
    }

    private long commentId(ResultActions created) throws Exception {
        String json = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).path("data").path("id").asLong();
    }
}
