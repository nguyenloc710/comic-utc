package vn.edu.utc.comic.interaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.chapter.entity.Chapter;
import vn.edu.utc.comic.common.entity.CreatedAtEntity;
import vn.edu.utc.comic.interaction.enums.CommentStatus;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.user.entity.UserAccount;

/** Bình luận ở trang truyện hoặc trang chương. Nội dung là text thuần, trả lời tối đa một cấp. */
@Getter
@Setter
@Entity
@Table(name = "comment")
public class Comment extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    /** Null = bình luận ở trang truyện, không gắn chương nào. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommentStatus status = CommentStatus.VISIBLE;

    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;
}
