package vn.edu.utc.comic.common.util;

import vn.edu.utc.comic.common.constant.ApiConstants;

/**
 * Dựng đường dẫn tới trang truyện và trang đọc, cho những nơi không đi qua template (link trong thông báo).
 */
public final class StoryLinks {

    private static final String PATH_SEPARATOR = "/";
    private static final String CHAPTERS_SEGMENT = "/chapters/";

    /** Neo tới khối bình luận trên trang truyện và trang đọc. */
    public static final String COMMENTS_ANCHOR = "#comments";

    public static String story(String storySlug) {
        return ApiConstants.STORIES_PATH + PATH_SEPARATOR + storySlug;
    }

    public static String chapter(String storySlug, int chapterNo) {
        return story(storySlug) + CHAPTERS_SEGMENT + chapterNo;
    }

    private StoryLinks() {
        throw new UnsupportedOperationException("Utility class");
    }
}
