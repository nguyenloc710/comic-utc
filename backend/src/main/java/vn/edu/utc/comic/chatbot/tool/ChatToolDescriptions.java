package vn.edu.utc.comic.chatbot.tool;

/**
 * Tên và mô tả các hàm tra cứu, cùng các mẩu văn bản dựng lịch sử hội thoại gửi cho mô hình.
 *
 * <p>Đây là văn bản cho MÔ HÌNH đọc, không phải chuỗi hiển thị cho người dùng, nên không nằm trong
 * messages.properties. Sửa mô tả hàm là cách chính để mô hình gọi hàm đúng hơn — chạy lại bộ câu hỏi đánh giá
 * (resources/chatbot/eval-queries.tsv) sau mỗi lần sửa.
 */
public final class ChatToolDescriptions {

    public static final String SEARCH_STORIES_NAME = "searchStories";
    public static final String SEARCH_STORIES = """
            Tìm truyện trong kho của website theo thể loại, loại truyện, trạng thái, độ dài và từ khóa. \
            Mọi tham số đều tùy chọn; chỉ điền những gì người dùng nêu. \
            Nếu không có truyện khớp, máy chủ tự nới bớt điều kiện và liệt kê điều kiện đã nới trong relaxedFilters.""";

    public static final String GET_STORY_DETAIL_NAME = "getStoryDetail";
    public static final String GET_STORY_DETAIL = """
            Xem chi tiết một truyện (mô tả đầy đủ hơn, tác giả, số chương, điểm, ngày cập nhật) theo id lấy từ \
            kết quả hàm khác. Dùng khi người dùng hỏi về nội dung một truyện cụ thể.""";
    public static final String STORY_ID_PARAM = "id của truyện, lấy từ kết quả hàm";

    public static final String FIND_SIMILAR_STORIES_NAME = "findSimilarStories";
    public static final String FIND_SIMILAR_STORIES = """
            Tìm truyện giống một truyện cho trước (chung nhiều thể loại nhất). Dùng khi người dùng muốn \
            "truyện giống truyện X": tìm X bằng searchStories trước để có id.""";

    public static final String GET_TRENDING_STORIES_NAME = "getTrendingStories";
    public static final String GET_TRENDING_STORIES = """
            Truyện được xem nhiều nhất gần đây. Dùng khi người dùng hỏi truyện hot, thịnh hành, nhiều người đọc, \
            hoặc khi người dùng chưa nêu yêu cầu gì cụ thể.""";
    public static final String PERIOD_PARAM = "DAY = hôm nay, WEEK = 7 ngày, MONTH = 30 ngày";
    public static final String TYPE_PARAM = "COMIC = truyện tranh, NOVEL = truyện chữ; bỏ trống nếu không phân biệt";
    public static final String LIMIT_PARAM = "Số truyện muốn nhận, tối đa 10";

    /** Tên các điều kiện máy chủ có thể nới khi tìm không ra (giá trị của relaxedFilters). */
    public static final String RELAXED_KEYWORD = "keyword";
    public static final String RELAXED_CHAPTER_RANGE = "chapterRange";
    public static final String RELAXED_STATUS = "status";

    /**
     * Dòng tóm tắt gắn sau mỗi lời đáp cũ của trợ lý trong lịch sử, để mô hình hiểu "ngắn hơn", "cái thứ hai"
     * mà không phải gửi lại toàn bộ kết quả hàm. {0}: các lần gọi hàm, {1}: các truyện đã gợi ý.
     */
    public static final String HISTORY_SUMMARY = "\n[Hàm đã gọi: {0}. Đã gợi ý: {1}]";
    /** Một truyện đã gợi ý trong dòng tóm tắt. {0}: id, {1}: tên, {2}: số chương. */
    public static final String HISTORY_STORY = "#{0} \"{1}\" ({2} chương)";
    public static final String HISTORY_NONE = "không có";
    public static final String HISTORY_SEPARATOR = "; ";

    private ChatToolDescriptions() {
        throw new UnsupportedOperationException("Utility class");
    }
}
