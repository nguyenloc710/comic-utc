package vn.edu.utc.comic.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.utc.comic.chatbot.service.ChatStatsService;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.SecurityConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;

/** Trang theo dõi chatbot của quản trị viên. Bật/tắt và hạn mức sửa ở trang tham số vận hành. */
@Controller
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AdminChatbotController {

    private static final String ATTR_STATS = "stats";

    private final ChatStatsService chatStatsService;

    /** Số liệu sử dụng và chất lượng của chatbot. */
    @GetMapping(ApiConstants.ADMIN_CHATBOT_PATH)
    public String showStats(Model model) {
        model.addAttribute(ATTR_STATS, chatStatsService.getStats());
        return ViewConstants.ADMIN_CHATBOT;
    }
}
