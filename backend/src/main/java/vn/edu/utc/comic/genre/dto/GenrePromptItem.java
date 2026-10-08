package vn.edu.utc.comic.genre.dto;

/** Một thể loại chèn vào system prompt để mô hình ánh xạ lời người dùng sang slug. */
public record GenrePromptItem(String slug, String name, String description) {
}
