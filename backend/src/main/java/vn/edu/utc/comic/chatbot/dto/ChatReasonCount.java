package vn.edu.utc.comic.chatbot.dto;

import vn.edu.utc.comic.chatbot.enums.FallbackReason;

/** Số lượt rơi về đường lui theo từng nguyên nhân. */
public record ChatReasonCount(FallbackReason reason, Long count) {
}
