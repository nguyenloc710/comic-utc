package vn.edu.utc.comic.common.security;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.utc.comic.common.constant.CacheConstants;
import vn.edu.utc.comic.user.repository.UserAccountRepository;

/**
 * Trạng thái hiện tại của tài khoản, đọc ở MỌI request đã đăng nhập nên phải cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountStateService {

    private final UserAccountRepository userAccountRepository;

    /**
     * @return trạng thái hiện tại; rỗng nếu tài khoản không còn tồn tại
     */
    @Cacheable(value = CacheConstants.ACCOUNT_STATES, key = "#userId")
    @Transactional(readOnly = true)
    public Optional<AccountState> findState(Long userId) {
        return userAccountRepository.findAccountStateById(userId);
    }

    /**
     * Bỏ bản cache của tài khoản vừa đổi.
     *
     * <p>Chạy SAU khi transaction commit: nếu xóa cache trước, một request khác của chính tài khoản đó chen vào
     * giữa sẽ đọc lại giá trị cũ (chưa commit) và cache nó thêm cả một chu kỳ — người vừa bị khóa vẫn dùng tiếp.
     * fallbackExecution để sự kiện phát ngoài transaction vẫn được xử lý.
     */
    @CacheEvict(value = CacheConstants.ACCOUNT_STATES, key = "#event.userId()")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleAccountChanged(AccountChangedEvent event) {
        log.debug("Làm mới trạng thái tài khoản {}", event.userId());
    }
}
