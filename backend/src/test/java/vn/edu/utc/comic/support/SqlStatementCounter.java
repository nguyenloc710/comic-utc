package vn.edu.utc.comic.support;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Đếm số câu SQL Hibernate gửi đi trên luồng hiện tại, để test phát hiện N+1 (số câu tăng theo số truyện).
 *
 * <p>Đếm theo luồng vì MockMvc xử lý request ngay trên luồng của test, còn việc chạy nền (ghi lượt xem, gửi thông
 * báo) ở luồng khác không được tính vào. Chỉ thấy câu do Hibernate phát; JdbcTemplate không đi qua đây.
 */
public class SqlStatementCounter implements StatementInspector {

    private static final ThreadLocal<Integer> COUNT = ThreadLocal.withInitial(() -> 0);

    @Override
    public String inspect(String sql) {
        COUNT.set(COUNT.get() + 1);
        return sql;
    }

    public static void reset() {
        COUNT.set(0);
    }

    public static int count() {
        return COUNT.get();
    }
}
