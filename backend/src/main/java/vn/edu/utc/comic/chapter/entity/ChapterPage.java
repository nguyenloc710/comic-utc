package vn.edu.utc.comic.chapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.edu.utc.comic.common.entity.BaseEntity;

/** Một trang ảnh của chương truyện tranh. Thứ tự trang (1..n) do service ghi lại mỗi lần sắp xếp. */
@Getter
@Setter
@Entity
@Table(name = "chapter_page")
public class ChapterPage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "page_no", nullable = false)
    private int pageNo;

    /** Khóa ảnh tương đối; URL dựng qua StorageService.resolveUrl. */
    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    /** Lưu kích thước để trang đọc đặt sẵn tỉ lệ khung, ảnh tải chậm không làm giật bố cục. */
    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
}
