package vn.edu.utc.comic.chatbot.dto;

import vn.edu.utc.comic.story.dto.StoryCardResponse;

/**
 * Một thẻ truyện trong câu trả lời của chatbot. Mọi thông tin trên thẻ dựng từ cơ sở dữ liệu; từ mô hình chỉ lấy
 * lý do gợi ý — nên tên, bìa và đường link luôn đúng dù mô hình viết gì.
 *
 * @param reason lý do gợi ý do mô hình viết; {@code null} với câu trả lời của đường lui
 */
public record ChatCardResponse(StoryCardResponse story, String reason) {
}
