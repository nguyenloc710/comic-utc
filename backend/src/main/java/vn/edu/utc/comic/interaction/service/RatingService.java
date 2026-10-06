package vn.edu.utc.comic.interaction.service;

import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.interaction.dto.RatingResponse;
import vn.edu.utc.comic.interaction.entity.StoryRating;
import vn.edu.utc.comic.interaction.entity.UserStoryId;
import vn.edu.utc.comic.interaction.repository.StoryRatingRepository;
import vn.edu.utc.comic.story.dto.StoryRatingSummary;
import vn.edu.utc.comic.story.entity.Story;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Đánh giá sao cho truyện. Mỗi người một lượt cho mỗi truyện, sửa được.
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    private final StoryRatingRepository storyRatingRepository;
    private final StoryRepository storyRepository;
    private final StoryCatalogQueryService storyCatalogQueryService;
    private final Clock clock;

    /**
     * Chấm hoặc sửa điểm cho một truyện đang công khai.
     *
     * <p>Tổng sao của truyện được cộng đúng phần CHÊNH LỆCH so với lượt chấm trước của chính người này,
     * và số lượt chỉ tăng khi đây là lượt đầu — sửa điểm bao nhiêu lần cũng không làm sai điểm trung bình.
     *
     * @param stars số sao từ 1 đến 5 (đã được kiểm tra ở tầng nhận request)
     * @throws ApiException STORY_NOT_FOUND nếu truyện không tồn tại hoặc không công khai,
     *                      RATING_OWN_STORY nếu tác giả tự đánh giá truyện của mình
     */
    @Transactional
    public RatingResponse rate(Long storyId, Long userId, int stars) {
        Story story = storyCatalogQueryService.getPublicStory(storyId);
        if (story.getAuthor().getId().equals(userId)) {
            throw new ApiException(ErrorCode.RATING_OWN_STORY);
        }
        storyRepository.lockForCounterUpdate(storyId);
        UserStoryId ratingId = new UserStoryId(userId, storyId);
        Optional<StoryRating> existing = storyRatingRepository.findById(ratingId);
        int previousStars = existing.map(StoryRating::getStars).orElse(0);

        StoryRating rating = existing.orElseGet(StoryRating::new);
        rating.setId(ratingId);
        rating.setStars(stars);
        rating.setUpdatedAt(clock.instant());
        storyRatingRepository.saveAndFlush(rating);
        storyRepository.addRating(storyId, stars - previousStars, existing.isPresent() ? 0 : 1);

        StoryRatingSummary summary = storyRepository.findRatingSummary(storyId);
        return new RatingResponse(stars, summary.average(), summary.ratingCount());
    }

    /** Số sao một người đã chấm cho truyện; rỗng nếu chưa đánh giá. */
    @Transactional(readOnly = true)
    public Optional<Integer> findStars(Long storyId, Long userId) {
        return storyRatingRepository.findById(new UserStoryId(userId, storyId)).map(StoryRating::getStars);
    }
}
