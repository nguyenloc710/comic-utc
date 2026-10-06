package vn.edu.utc.comic.story.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;

/** Trang chủ công khai. Các khối truyện (mới cập nhật, nổi bật, đọc tiếp) được bổ sung ở giai đoạn 3. */
@Controller
public class HomeController {

    /** Hiển thị trang chủ; khách vãng lai cũng xem được. */
    @GetMapping(ApiConstants.HOME_PATH)
    public String showHome() {
        return ViewConstants.HOME;
    }
}
