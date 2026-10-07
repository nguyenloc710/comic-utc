package vn.edu.utc.comic.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.comic.common.constant.ApiConstants;
import vn.edu.utc.comic.common.constant.MessageKeys;
import vn.edu.utc.comic.common.dto.PageResponse;
import vn.edu.utc.comic.common.exception.ApiException;
import vn.edu.utc.comic.common.exception.ErrorCode;
import vn.edu.utc.comic.common.i18n.MessageService;
import vn.edu.utc.comic.notification.dto.NotificationResponse;
import vn.edu.utc.comic.notification.entity.Notification;
import vn.edu.utc.comic.notification.enums.NotificationType;
import vn.edu.utc.comic.notification.mapper.NotificationMapper;
import vn.edu.utc.comic.notification.repository.NotificationRepository;

/**
 * Ghi và đọc thông báo trong ứng dụng.
 *
 * <p>Thông báo lưu LOẠI + THAM SỐ chứ không lưu câu chữ: câu hiển thị được dựng từ messages.properties lúc
 * đọc, nên sửa lời văn không phải sửa dữ liệu đã ghi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String PROTOCOL_RELATIVE_PREFIX = "//";
    private static final String PATH_PREFIX = "/";

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Ghi một thông báo cho một người.
     *
     * @param link      đường dẫn tương đối mở ra khi bấm vào thông báo
     * @param arguments tham số điền vào câu {@code notification.<TYPE>} theo thứ tự {0}, {1}...
     */
    @Transactional
    public void notifyUser(Long recipientId, NotificationType type, String link, String... arguments) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setMessageArgs(toJson(arguments));
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    /**
     * Ghi cùng một thông báo cho mọi người đang theo dõi một truyện.
     *
     * @return số người nhận
     */
    @Transactional
    public int notifyFollowers(Long storyId, NotificationType type, String link, String... arguments) {
        int recipients = notificationRepository.insertForFollowers(storyId, type.name(), toJson(arguments), link,
                clock.instant());
        log.debug("Đã gửi thông báo {} của truyện {} tới {} người theo dõi", type, storyId, recipients);
        return recipients;
    }

    /**
     * Thông báo của một người, mới nhất trước.
     *
     * @param page chỉ số trang, bắt đầu từ 0; giá trị âm được coi là 0
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> findNotifications(Long userId, int page) {
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByIdDesc(userId,
                PageRequest.of(Math.max(page, 0), ApiConstants.NOTIFICATION_PAGE_SIZE));
        return PageResponse.of(notifications, content -> content.stream()
                .map(notification -> notificationMapper.toResponse(notification, buildMessage(notification)))
                .toList());
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    /**
     * Đánh dấu một thông báo là đã đọc và trả về nơi cần chuyển tới.
     *
     * @return đường dẫn của thông báo; thông báo không có đường dẫn (hoặc đường dẫn không phải đường dẫn nội bộ)
     *         thì quay về trang danh sách thông báo
     * @throws ApiException NOTIFICATION_NOT_FOUND nếu không có hoặc là thông báo của người khác
     */
    @Transactional
    public String openNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.setRead(true);
        return isInternalPath(notification.getLink()) ? notification.getLink() : ApiConstants.NOTIFICATIONS_PATH;
    }

    /** @return số thông báo vừa được đánh dấu đã đọc */
    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    /**
     * Chỉ chuyển hướng tới đường dẫn bên trong ứng dụng. Đường dẫn do máy chủ tự ghi nên vốn đã an toàn;
     * kiểm tra lại ở đây để dữ liệu hỏng không bao giờ biến trang thông báo thành chỗ chuyển hướng ra ngoài.
     */
    private static boolean isInternalPath(String link) {
        return link != null && link.startsWith(PATH_PREFIX) && !link.startsWith(PROTOCOL_RELATIVE_PREFIX);
    }

    private String buildMessage(Notification notification) {
        return messageService.getMessage(MessageKeys.NOTIFICATION_PREFIX + notification.getType().name(),
                (Object[]) parseArguments(notification));
    }

    private String toJson(String[] arguments) {
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException exception) {
            // Mảng chuỗi luôn chuyển được sang JSON; nếu vẫn hỏng thì đó là lỗi lập trình, không nuốt
            throw new IllegalStateException("Không chuyển được tham số thông báo sang JSON", exception);
        }
    }

    /** Tham số hỏng thì vẫn hiện được câu thông báo (thiếu phần điền) thay vì làm hỏng cả trang. */
    private String[] parseArguments(Notification notification) {
        if (notification.getMessageArgs() == null) {
            return new String[0];
        }
        try {
            return objectMapper.readValue(notification.getMessageArgs(), String[].class);
        } catch (JsonProcessingException exception) {
            log.warn("Tham số của thông báo {} không đọc được", notification.getId(), exception);
            return new String[0];
        }
    }
}
