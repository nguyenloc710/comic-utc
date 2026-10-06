package vn.edu.utc.comic.interaction.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.interaction.dto.ReadingHistoryItemResponse;
import vn.edu.utc.comic.interaction.repository.ReadingHistoryRepository;
import vn.edu.utc.comic.story.mapper.StoryMapper;

/**
 * Tiến độ đọc: mỗi người có một dòng cho mỗi truyện, ghi chương đang đọc dở.
 */
@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final StoryMapper storyMapper;
    private final Clock clock;

    /** Ghi lại chương một người vừa mở đọc; lần mở sau ghi đè lần trước. */
    @Transactional
    public void recordProgress(Long userId, Long storyId, Long chapterId) {
        readingHistoryRepository.saveProgress(userId, storyId, chapterId, clock.instant());
    }

    /** Số của chương đang đọc dở ở một truyện; rỗng nếu chưa đọc hoặc chương đó không còn công khai. */
    @Transactional(readOnly = true)
    public Optional<Integer> findContinueChapterNo(Long userId, Long storyId) {
        return readingHistoryRepository.findCurrentChapterNo(userId, storyId);
    }

    /** Các truyện một người đọc gần đây nhất, mới đọc trước; truyện không còn công khai bị bỏ qua. */
    @Transactional(readOnly = true)
    public List<ReadingHistoryItemResponse> findRecent(Long userId, int limit) {
        return readingHistoryRepository.findRecent(userId, PageRequest.of(0, limit)).stream()
                .map(row -> new ReadingHistoryItemResponse(storyMapper.toCard(row.story()), row.chapterNo(),
                        row.readAt()))
                .toList();
    }
}
