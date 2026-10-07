package vn.edu.utc.comic.interaction.service;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.interaction.dto.FollowResponse;
import vn.edu.utc.comic.interaction.entity.UserStoryId;
import vn.edu.utc.comic.interaction.repository.StoryFollowRepository;
import vn.edu.utc.comic.story.repository.StoryRepository;
import vn.edu.utc.comic.story.service.StoryCatalogQueryService;

/**
 * Theo dõi và bỏ theo dõi truyện.
 *
 * <p>Cả hai thao tác đều lặp lại được mà không sai số: bộ đếm của truyện chỉ đổi khi cơ sở dữ liệu báo là
 * thật sự vừa thêm hoặc vừa xóa một dòng, nên bấm đúp hay hai tab cùng gửi không làm lệch số người theo dõi.
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final StoryFollowRepository storyFollowRepository;
    private final StoryRepository storyRepository;
    private final StoryCatalogQueryService storyCatalogQueryService;
    private final Clock clock;

    /**
     * Theo dõi một truyện đang công khai; đã theo dõi rồi thì không có gì thay đổi.
     *
     * @throws ApiException STORY_NOT_FOUND nếu truyện không tồn tại hoặc không công khai
     */
    @Transactional
    public FollowResponse follow(Long storyId, Long userId) {
        storyRepository.lockForCounterUpdate(storyId);
        storyCatalogQueryService.getPublicStory(storyId);
        if (storyFollowRepository.insertIfAbsent(userId, storyId, clock.instant()) > 0) {
            storyRepository.addFollowCount(storyId, 1);
        }
        return new FollowResponse(true, getFollowCount(storyId));
    }

    /**
     * Bỏ theo dõi; vốn không theo dõi thì không có gì thay đổi. Cho phép cả khi truyện đã bị ẩn,
     * để người dùng dọn được tủ truyện của mình.
     *
     * @throws ApiException STORY_NOT_FOUND nếu truyện không tồn tại
     */
    @Transactional
    public FollowResponse unfollow(Long storyId, Long userId) {
        if (storyFollowRepository.deleteFollow(userId, storyId) > 0) {
            storyRepository.addFollowCount(storyId, -1);
        }
        return new FollowResponse(false, getFollowCount(storyId));
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long storyId, Long userId) {
        return storyFollowRepository.existsById(new UserStoryId(userId, storyId));
    }

    /** Đọc lại từ cơ sở dữ liệu sau khi cộng dồn, vì thực thể trong bộ nhớ (nếu có) vẫn mang số cũ. */
    private int getFollowCount(Long storyId) {
        return storyRepository.findFollowCount(storyId)
                .orElseThrow(() -> new ApiException(ErrorCode.STORY_NOT_FOUND));
    }
}
