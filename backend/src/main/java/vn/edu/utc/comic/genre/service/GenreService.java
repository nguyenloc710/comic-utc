package vn.edu.utc.comic.genre.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.constant.CacheConstants;
import vn.edu.utc.comic.common.constant.GenreConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.exception.FieldValidationException;
import vn.edu.utc.comic.common.exception.FieldValidationException.FieldViolation;
import vn.edu.utc.comic.common.util.SlugUtils;
import vn.edu.utc.comic.genre.dto.GenreForm;
import vn.edu.utc.comic.genre.dto.GenreResponse;
import vn.edu.utc.comic.genre.dto.GenreStoryCount;
import vn.edu.utc.comic.genre.dto.GenreTagResponse;
import vn.edu.utc.comic.genre.entity.Genre;
import vn.edu.utc.comic.genre.mapper.GenreMapper;
import vn.edu.utc.comic.genre.repository.GenreRepository;

/**
 * Quản lý thể loại truyện.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    private static final String FIELD_NAME = "name";

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    /** Toàn bộ thể loại (kể cả đang tắt) theo thứ tự hiển thị, kèm số truyện đang gắn. */
    @Transactional(readOnly = true)
    public List<GenreResponse> findAllForAdmin() {
        Map<Long, Long> storyCountByGenreId = genreRepository.countStoriesByGenre().stream()
                .collect(Collectors.toMap(GenreStoryCount::genreId, GenreStoryCount::storyCount));
        return genreRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(genre -> genreMapper.toResponse(genre, storyCountByGenreId.getOrDefault(genre.getId(), 0L)))
                .toList();
    }

    /**
     * Thể loại đang bật theo thứ tự hiển thị, cho bộ lọc truyện. Đọc ở mọi lần mở trang danh sách nên được cache;
     * các phương thức tạo / sửa / xóa bên dưới xóa cache này.
     */
    @Cacheable(CacheConstants.GENRES)
    @Transactional(readOnly = true)
    public List<GenreTagResponse> findActiveGenres() {
        return genreMapper.toTags(genreRepository.findByActiveTrueOrderBySortOrderAscNameAsc());
    }

    /**
     * Một thể loại đang bật theo slug trên URL.
     *
     * @throws ApiException GENRE_NOT_FOUND nếu không có hoặc đã bị tắt
     */
    @Transactional(readOnly = true)
    public GenreTagResponse getActiveGenre(String slug) {
        return genreRepository.findBySlugAndActiveTrue(slug)
                .map(genreMapper::toTag)
                .orElseThrow(() -> new ApiException(ErrorCode.GENRE_NOT_FOUND));
    }

    /**
     * Form sửa điền sẵn giá trị hiện tại.
     *
     * @throws ApiException GENRE_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public GenreForm getForm(Long genreId) {
        return genreMapper.toForm(getGenre(genreId));
    }

    /**
     * Tạo thể loại; slug sinh từ tên (bỏ dấu) và cố định từ đây vì nó nằm trên URL và trong tham số hàm
     * của chatbot.
     *
     * @return tên thể loại đã chuẩn hóa
     * @throws FieldValidationException tên trùng, hoặc tên sinh ra slug rỗng / trùng slug của thể loại khác
     */
    @CacheEvict(value = CacheConstants.GENRES, allEntries = true)
    @Transactional
    public String createGenre(GenreForm form) {
        String name = form.getName().trim();
        String slug = SlugUtils.toSlug(name, GenreConstants.SLUG_MAX_LENGTH);
        if (slug.isEmpty()) {
            throw nameViolation(MessageKeys.ERROR_GENRE_NAME_INVALID);
        }
        // Hai tên khác nhau vẫn có thể ra cùng slug ("Tu Tiên" và "Tu-tien"), nên phải kiểm tra cả hai
        if (genreRepository.existsByName(name) || genreRepository.existsBySlug(slug)) {
            throw nameViolation(MessageKeys.ERROR_GENRE_NAME_DUPLICATED);
        }
        Genre genre = new Genre();
        genreMapper.updateFromForm(form, genre);
        genre.setName(name);
        genre.setSlug(slug);
        genreRepository.save(genre);
        log.info("Đã tạo thể loại {} ({})", genre.getId(), slug);
        return name;
    }

    /**
     * Sửa tên, mô tả, thứ tự, bật/tắt. Slug giữ nguyên dù đổi tên để link cũ không chết.
     *
     * @throws ApiException             GENRE_NOT_FOUND
     * @throws FieldValidationException tên trùng với thể loại khác
     */
    @CacheEvict(value = CacheConstants.GENRES, allEntries = true)
    @Transactional
    public void updateGenre(Long genreId, GenreForm form) {
        Genre genre = getGenre(genreId);
        String name = form.getName().trim();
        if (genreRepository.existsByNameAndIdNot(name, genreId)) {
            throw nameViolation(MessageKeys.ERROR_GENRE_NAME_DUPLICATED);
        }
        genreMapper.updateFromForm(form, genre);
        genre.setName(name);
    }

    /**
     * Xóa hẳn một thể loại chưa từng được gắn cho truyện nào.
     *
     * @throws ApiException GENRE_NOT_FOUND; GENRE_IN_USE nếu còn truyện gắn thể loại — khi đó chỉ tắt được
     */
    @CacheEvict(value = CacheConstants.GENRES, allEntries = true)
    @Transactional
    public void deleteGenre(Long genreId) {
        Genre genre = getGenre(genreId);
        long storyCount = genreRepository.countStoriesUsing(genreId);
        if (storyCount > 0) {
            throw new ApiException(ErrorCode.GENRE_IN_USE, storyCount);
        }
        genreRepository.delete(genre);
        log.info("Đã xóa thể loại {} ({})", genreId, genre.getSlug());
    }

    private static FieldValidationException nameViolation(String messageKey) {
        return new FieldValidationException(List.of(new FieldViolation(FIELD_NAME, messageKey)));
    }

    private Genre getGenre(Long genreId) {
        return genreRepository.findById(genreId)
                .orElseThrow(() -> new ApiException(ErrorCode.GENRE_NOT_FOUND));
    }
}
