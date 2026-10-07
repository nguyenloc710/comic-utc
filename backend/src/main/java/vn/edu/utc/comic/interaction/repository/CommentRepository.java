package vn.edu.utc.comic.interaction.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.utc.comic.interaction.entity.Comment;
import vn.edu.utc.comic.interaction.enums.CommentStatus;

/**
 * Truy vấn bình luận. Các truy vấn danh sách nạp luôn người viết (JOIN FETCH) để hiển thị tên và ảnh đại diện
 * mà không phát sinh một truy vấn cho mỗi bình luận.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Bình luận gốc ở trang truyện (không gắn chương nào), mới nhất trước. */
    @Query(value = """
            SELECT c FROM Comment c JOIN FETCH c.user
            WHERE c.story.id = :storyId AND c.chapter IS NULL AND c.parent IS NULL
            ORDER BY c.createdAt DESC, c.id DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Comment c
            WHERE c.story.id = :storyId AND c.chapter IS NULL AND c.parent IS NULL
            """)
    Page<Comment> findStoryThreads(@Param("storyId") Long storyId, Pageable pageable);

    /** Bình luận gốc của một chương, mới nhất trước. */
    @Query(value = """
            SELECT c FROM Comment c JOIN FETCH c.user
            WHERE c.story.id = :storyId AND c.chapter.id = :chapterId AND c.parent IS NULL
            ORDER BY c.createdAt DESC, c.id DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Comment c
            WHERE c.story.id = :storyId AND c.chapter.id = :chapterId AND c.parent IS NULL
            """)
    Page<Comment> findChapterThreads(@Param("storyId") Long storyId, @Param("chapterId") Long chapterId,
                                     Pageable pageable);

    /** Mọi câu trả lời của một nhóm bình luận gốc trong một truy vấn, cũ nhất trước. */
    @Query("""
            SELECT c FROM Comment c JOIN FETCH c.user
            WHERE c.parent.id IN :parentIds
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<Comment> findReplies(@Param("parentIds") Collection<Long> parentIds);

    @Query("SELECT MAX(c.createdAt) FROM Comment c WHERE c.user.id = :userId")
    Optional<Instant> findLastCommentTime(@Param("userId") Long userId);

    /** Chỉ id truyện của bình luận, để khóa dòng truyện trước khi nạp gì khác. */
    @Query("SELECT c.story.id FROM Comment c WHERE c.id = :commentId")
    Optional<Long> findStoryIdById(@Param("commentId") Long commentId);

    /**
     * Bình luận cho màn kiểm duyệt (mọi trạng thái), mới nhất trước; tham số null nghĩa là không lọc.
     * Từ khóa đã được service cắt khoảng trắng; LIKE không thoát ký tự đại diện vì đây là màn của quản trị viên.
     */
    @Query(value = """
            SELECT c FROM Comment c JOIN FETCH c.user JOIN FETCH c.story LEFT JOIN FETCH c.chapter
            WHERE (:status IS NULL OR c.status = :status)
              AND (:keyword IS NULL OR c.content LIKE CONCAT('%', :keyword, '%'))
            ORDER BY c.id DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Comment c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:keyword IS NULL OR c.content LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Comment> findForAdmin(@Param("status") CommentStatus status, @Param("keyword") String keyword,
                               Pageable pageable);
}
