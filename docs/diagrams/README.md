# Sơ đồ phân tích thiết kế (PlantUML)

Nguồn sơ đồ cho Chương 3 của báo cáo. Tên lớp, bảng, cột và luồng xử lý lấy đúng theo mã nguồn
(`backend/src/main/java`, `db/migration/V1__init_schema.sql`, `V3__chatbot.sql`); sửa code thì sửa sơ đồ theo.

| Tệp | Nội dung |
|---|---|
| `usecase.puml` | Use case tổng quát: khách, độc giả, tác giả, quản trị viên (kế thừa khách → độc giả → tác giả / quản trị) và job định kỳ |
| `erd.puml` | ERD đủ 21 bảng (V1 + V3), khóa chính / ngoại, giá trị của các cột trạng thái; không vẽ cột `created_by` / `updated_by` |
| `class-domain.puml` | Lớp miền chính (JPA entity) và enum trạng thái, máy trạng thái của chương |
| `class-services.puml` | Các service "một cửa" và quan hệ phụ thuộc giữa chúng |
| `seq-author-request.puml` | Gửi yêu cầu làm tác giả → duyệt → cấp quyền → `AuthorityRefreshFilter` làm mới phiên đang mở |
| `seq-publish-chapter.puml` | Đăng ngay / hẹn giờ → `ChapterPublishJob` (UPDATE có điều kiện) → thông báo người theo dõi sau commit |
| `seq-read-chapter.puml` | Đọc chương: kiểm tra công khai (404), đếm lượt xem ba transaction, lưu tiến độ đọc |
| `seq-chatbot.puml` | Chatbot: hai transaction ngắn quanh lời gọi mô hình, gọi hàm tra cứu, hậu kiểm gợi ý, đường lui |

## Xuất ảnh PNG

```bash
bash docs/diagrams/render.sh        # cần Java 17+; ảnh ra docs/diagrams/out/ (không đưa vào Git)
```

Script tải PlantUML (phiên bản ghim trong script) từ Maven Central ở lần chạy đầu. Sơ đồ dạng đồ thị dùng bộ dàn trang
`smetana` có sẵn trong PlantUML nên không cần cài Graphviz. Phông Arial; ký tự `⇒` không có trong Arial (ra ô vuông),
dùng `→` thay thế.
