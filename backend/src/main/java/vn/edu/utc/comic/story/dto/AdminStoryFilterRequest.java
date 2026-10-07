package vn.edu.utc.comic.story.dto;

import vn.edu.utc.comic.story.enums.StoryType;
import vn.edu.utc.comic.story.enums.StoryVisibility;

/**
 * Bộ lọc danh sách truyện ở trang quản trị. Trường để trống nghĩa là không lọc theo trường đó.
 *
 * @param keyword một phần tên truyện hoặc slug
 */
public record AdminStoryFilterRequest(String keyword, StoryType type, StoryVisibility visibility) {
}
