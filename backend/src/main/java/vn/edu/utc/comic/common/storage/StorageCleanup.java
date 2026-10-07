package vn.edu.utc.comic.common.storage;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Xóa tệp ảnh không còn được bản ghi nào trỏ tới, nhưng chỉ SAU KHI transaction commit.
 *
 * <p>Xóa ngay mà transaction rollback thì bản ghi vẫn trỏ tới một tệp không còn. Chiều ngược lại (commit hỏng
 * sau khi đã lưu ảnh mới) chỉ để lại một tệp mồ côi, tốn chút dung lượng đĩa chứ không làm hỏng dữ liệu.
 */
@Component
@RequiredArgsConstructor
public class StorageCleanup {

    private final StorageService storageService;

    /** Hẹn xóa một tệp; khóa {@code null} (chưa từng có ảnh) được bỏ qua. */
    public void deleteAfterCommit(String storageKey) {
        if (storageKey != null) {
            deleteAfterCommit(List.of(storageKey));
        }
    }

    /** Hẹn xóa nhiều tệp. Phải gọi bên trong một transaction đang mở. */
    public void deleteAfterCommit(Collection<String> storageKeys) {
        List<String> keys = List.copyOf(storageKeys);
        if (keys.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(storageService::delete);
            }
        });
    }
}
