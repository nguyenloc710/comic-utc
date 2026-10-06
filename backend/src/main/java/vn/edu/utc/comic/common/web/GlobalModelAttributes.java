package vn.edu.utc.comic.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.utc.comic.common.constant.DateTimeConstants;
import vn.edu.utc.comic.common.constant.ViewConstants;
import vn.edu.utc.comic.common.security.AppUserPrincipal;
import vn.edu.utc.comic.common.security.SecurityUtils;
import vn.edu.utc.comic.common.storage.StorageService;

/**
 * Thuộc tính có mặt ở mọi template: người dùng hiện tại, múi giờ hiển thị, đường dẫn đang mở.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private static final String PAGE_PARAMETER_PREFIX = "page=";
    private static final String QUERY_SEPARATOR = "&";

    private final StorageService storageService;

    /** Người đang đăng nhập; {@code null} với khách vãng lai. */
    @ModelAttribute(ViewConstants.ATTR_CURRENT_USER)
    public AppUserPrincipal currentUser() {
        return SecurityUtils.getCurrentPrincipal().orElse(null);
    }

    /** URL ảnh đại diện của người đang đăng nhập; {@code null} khi là khách hoặc chưa đặt ảnh. */
    @ModelAttribute(ViewConstants.ATTR_CURRENT_USER_AVATAR_URL)
    public String currentUserAvatarUrl() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> storageService.resolveUrl(principal.getAvatarPath()))
                .orElse(null);
    }

    @ModelAttribute(ViewConstants.ATTR_DISPLAY_ZONE)
    public ZoneId displayZone() {
        return DateTimeConstants.DISPLAY_ZONE;
    }

    /** Đường dẫn hiện tại, để layout tô đậm mục menu đang mở và fragment phân trang dựng link. */
    @ModelAttribute(ViewConstants.ATTR_REQUEST_PATH)
    public String requestPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /**
     * Query string hiện tại đã bỏ tham số page (kết thúc bằng '&' nếu có nội dung),
     * để link phân trang giữ nguyên bộ lọc người dùng đã chọn.
     */
    @ModelAttribute(ViewConstants.ATTR_FILTER_QUERY)
    public String filterQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return "";
        }
        String kept = Arrays.stream(query.split(QUERY_SEPARATOR))
                .filter(pair -> !pair.startsWith(PAGE_PARAMETER_PREFIX))
                .collect(Collectors.joining(QUERY_SEPARATOR));
        return kept.isEmpty() ? "" : kept + QUERY_SEPARATOR;
    }
}
