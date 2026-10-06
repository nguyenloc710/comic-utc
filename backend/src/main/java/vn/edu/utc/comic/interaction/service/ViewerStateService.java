package vn.edu.utc.comic.interaction.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.security.Viewer;
import vn.edu.utc.comic.interaction.dto.StoryViewerStateResponse;

/**
 * Gom quan hệ của người đang xem với một truyện (theo dõi, đánh giá, chương đọc dở) cho trang chi tiết.
 */
@Service
@RequiredArgsConstructor
public class ViewerStateService {

    private final FollowService followService;
    private final RatingService ratingService;
    private final ReadingHistoryService readingHistoryService;

    /** Khách vãng lai luôn nhận trạng thái trống mà không phát sinh truy vấn nào. */
    @Transactional(readOnly = true)
    public StoryViewerStateResponse getState(Long storyId, Viewer viewer) {
        if (!viewer.isAuthenticated()) {
            return StoryViewerStateResponse.none();
        }
        Long userId = viewer.userId();
        return new StoryViewerStateResponse(
                followService.isFollowing(storyId, userId),
                ratingService.findStars(storyId, userId).orElse(null),
                readingHistoryService.findContinueChapterNo(userId, storyId).orElse(null));
    }
}
