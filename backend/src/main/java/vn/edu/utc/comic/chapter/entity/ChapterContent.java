package vn.edu.utc.comic.chapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Nội dung chương truyện chữ, dùng chung khóa chính với {@link Chapter}.
 * Cột content là HTML ĐÃ làm sạch bằng whitelist lúc lưu — nơi duy nhất template được dùng th:utext.
 */
@Getter
@Setter
@Entity
@Table(name = "chapter_content")
public class ChapterContent {

    @Id
    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;
}
