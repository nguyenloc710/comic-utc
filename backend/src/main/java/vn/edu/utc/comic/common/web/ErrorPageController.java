package vn.edu.utc.comic.common.web;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.i18n.MessageService;

/** Trang 403 do Spring Security chuyển tiếp tới (accessDeniedPage). */
@Controller
@RequiredArgsConstructor
public class ErrorPageController {

    private final MessageService messageService;

    /**
     * Nhận MỌI phương thức, không riêng GET.
     *
     * <p>Spring Security chuyển tiếp (forward) chính request gốc sang đây, mà forward giữ nguyên
     * phương thức. Khi chỉ khai báo GET thì một POST bị chặn — thiếu CSRF token, hoặc thiếu quyền —
     * sẽ thành 405 rồi rơi vào nhánh "lỗi không lường trước" và trả về 500 thay vì 403.
     */
    @RequestMapping(ApiConstants.FORBIDDEN_PAGE_PATH)
    public String forbidden(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute(ViewConstants.ATTR_ERROR_STATUS, HttpStatus.FORBIDDEN.value());
        model.addAttribute(ViewConstants.ATTR_ERROR_MESSAGE,
                messageService.getMessage(ErrorCode.FORBIDDEN.getMessageKey()));
        return ViewConstants.ERROR_PAGE;
    }
}
