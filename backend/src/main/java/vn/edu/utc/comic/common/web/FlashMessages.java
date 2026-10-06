package vn.edu.utc.comic.common.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.i18n.MessageService;

/**
 * Đưa thông báo đã dịch vào flash attribute cho mẫu PRG (POST → redirect → GET).
 */
@Component
@RequiredArgsConstructor
public class FlashMessages {

    private final MessageService messageService;

    public void success(RedirectAttributes redirect, String messageKey, Object... arguments) {
        redirect.addFlashAttribute(ViewConstants.ATTR_FLASH_SUCCESS, messageService.getMessage(messageKey, arguments));
    }

    /** Lỗi nghiệp vụ trong form POST: hiện ngay trên trang vừa redirect về thay vì trang lỗi chung. */
    public void error(RedirectAttributes redirect, ApiException exception) {
        redirect.addFlashAttribute(ViewConstants.ATTR_FLASH_ERROR,
                messageService.getMessage(exception.getErrorCode().getMessageKey(), exception.getArguments()));
    }
}
