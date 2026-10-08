# comic-utc

Website đọc và đăng tải truyện tranh, truyện chữ tích hợp chatbot AI gợi ý truyện — đồ án tốt nghiệp, Khoa CNTT, Trường ĐH GTVT.

- **Front-end:** Thymeleaf (+ Layout Dialect), HTML/CSS, Bootstrap 5, JavaScript
- **Back-end:** Java 21 + Spring Boot 3.5 (Spring MVC, Spring Security, Spring Data JPA / Hibernate)
- **CSDL:** MySQL 8.4 (Flyway quản lý schema)
- **Chatbot:** Spring AI + function calling
- **Vai trò:** khách vãng lai, độc giả (`USER`), tác giả (`AUTHOR`), quản trị viên (`ADMIN`)

> Trạng thái: xong **giai đoạn 6** — chatbot gợi ý truyện (Spring AI function calling, 4 hàm tra cứu chỉ đọc, hậu kiểm gợi ý, lịch sử hỏi nối tiếp, đường lui tìm theo thể loại/từ khóa khi mô hình lỗi, hạn mức ngày, trang `/admin/chatbot`). Bộ câu hỏi đánh giá đã có nhưng chưa chạy với mô hình thật (cần `AI_API_KEY`). Trước đó: giai đoạn 5 — quản trị hoàn chỉnh: kiểm duyệt truyện / chương / bình luận (ẩn kèm lý do, tác giả được báo), độc giả báo cáo vi phạm và quản trị viên xử lý trong hàng đợi, đổi vai trò tài khoản, dashboard KPI + biểu đồ, sửa tham số vận hành tại chỗ, nhật ký kiểm toán. Trước đó: vòng đời tác giả (GĐ 4), phía độc giả (GĐ 3), tài khoản (GĐ 2). Tiếp theo: giai đoạn 7 (kiểm thử, triển khai). Tiến độ chi tiết: [docs/02-LO-TRINH.md](docs/02-LO-TRINH.md).

## Chạy dự án

Yêu cầu: **JDK 21**, **Docker Desktop** (đang chạy), Git. Không cần cài Maven: dùng `mvnw` đi kèm.

```bash
docker compose up -d                     # MySQL (3308), Adminer (8082); chờ ~30s cho MySQL healthy
cd backend && ./mvnw spring-boot:run     # Windows CMD/PowerShell: mvnw.cmd spring-boot:run
```

Mở http://localhost:8080. Profile mặc định là `dev`: Flyway tạo lược đồ (`db/migration`) rồi nạp tài khoản và 65 truyện demo (`db/demo`), trong đó truyện tranh **Hạt Tiêu & Cà Rốt** là 6 tập thật của webcomic Pepper&Carrot (David Revoy, CC BY 4.0, bản dịch tiếng Việt của Binh Pham).
`.env` là tùy chọn (compose và ứng dụng đã có giá trị mặc định khớp nhau); chỉ cần `cp .env.example .env` khi muốn đổi cổng, mật khẩu DB hoặc đặt khóa API cho chatbot.

| Tài khoản | Mật khẩu | Vai trò | Vào được |
|---|---|---|---|
| `admin` | `Admin@123` | Quản trị viên | `/admin` (người dùng, thể loại) |
| `author1` … `author3` | `Demo@123` | Tác giả | `/studio` (truyện, chương, thống kê); xem trước được truyện nháp/bị ẩn của chính mình |
| `reader1` … `reader15` | `Demo@123` | Độc giả | theo dõi, đánh giá, bình luận, báo cáo vi phạm, `/me/library`, `/me/history`, `/me/notifications`, `/me/author-request` |

Hoặc tự tạo tài khoản độc giả ở `/register`.

Tài khoản `admin` nằm trong dữ liệu khởi tạo của mọi môi trường — **phải đổi mật khẩu trước khi triển khai thật**.

| Địa chỉ | Nội dung |
|---|---|
| http://localhost:8080 | Trang chủ |
| http://localhost:8080/stories · `/genres/{slug}` · `/rankings` | Tìm kiếm và lọc truyện · truyện theo thể loại · bảng xếp hạng |
| http://localhost:8080/stories/{slug} · `…/chapters/{số}` | Chi tiết truyện · trang đọc |
| http://localhost:8080/me/library · `/me/history` | Tủ truyện đang theo dõi · lịch sử đọc |
| http://localhost:8080/me/notifications · `/me/author-request` | Thông báo · đăng ký làm tác giả |
| http://localhost:8080/studio/stories · `/studio/stats` | Tác giả: truyện và chương · thống kê |
| http://localhost:8080/admin/author-requests | Quản trị: duyệt yêu cầu làm tác giả |
| http://localhost:8080/admin/stories · `/admin/comments` · `/admin/reports` | Quản trị: kiểm duyệt truyện và chương · bình luận · hàng đợi báo cáo vi phạm |
| http://localhost:8080/admin/settings · `/admin/audit-logs` | Quản trị: tham số vận hành · nhật ký kiểm toán |
| http://localhost:8080/admin/chatbot | Quản trị: số liệu chatbot (lượng dùng, tỉ lệ đường lui, độ trễ, token, phản hồi) |
| http://localhost:8080/login · `/register` | Đăng nhập · đăng ký |
| http://localhost:8080/me/profile | Hồ sơ cá nhân, đổi mật khẩu |
| http://localhost:8080/studio | Khu vực tác giả |
| http://localhost:8080/admin | Quản trị: tổng quan (KPI, biểu đồ), người dùng, thể loại |
| http://localhost:8080/swagger-ui.html | Tài liệu API JSON (`/api/**`, cần quyền ADMIN) |
| http://localhost:8080/actuator/health | Kiểm tra tình trạng hệ thống |
| http://localhost:8082 | Adminer — xem CSDL (server `mysql`, user `comic`, mật khẩu `comic_local_pwd`, DB `comic_utc`) |

### Chatbot

Khung chat (nút tròn góc phải) hiện cho mọi người đã đăng nhập. Không đặt `AI_API_KEY` thì chatbot vẫn chạy ở chế độ
đường lui: lọc theo thể loại / loại truyện nhận ra trong câu hoặc tìm theo từ khóa. Đặt khóa (và tùy chọn `AI_MODEL`,
`AI_READ_TIMEOUT`) trong biến môi trường hoặc `.env` để dùng mô hình thật. Bật/tắt và hạn mức ở `/admin/settings`.

### Kiểm thử

```bash
cd backend && ./mvnw verify              # unit test + test tích hợp (Testcontainers MySQL) + báo cáo JaCoCo
```

Test gọi LLM thật (`SpringAiToolCallingSmokeTest`) tự bỏ qua khi chưa có khóa; để chạy: đặt biến môi trường `AI_API_KEY` rồi `./mvnw test -Dtest=SpringAiToolCallingSmokeTest`.

Dữ liệu demo sinh bằng `node backend/scripts/generate-demo-data.mjs` (ghi đè `db/demo/V101__demo_content.sql` và ảnh trong `resources/demo-media`). Tệp SQL đã nằm sẵn trong repo; chỉ chạy lại script khi muốn đổi nội dung demo, và khi đó phải nạp lại dữ liệu từ đầu vì Flyway đã ghi nhận tệp cũ.
Truyện Pepper&Carrot (`db/demo/V103__demo_peppercarrot.sql`, ảnh ở `resources/demo-media/demo/peppercarrot`) có nguồn gốc, giấy phép và cách dựng lại ghi ở [backend/scripts/peppercarrot/README.md](backend/scripts/peppercarrot/README.md).

### Nạp lại dữ liệu từ đầu

```bash
docker compose down -v && docker compose up -d      # xóa volume MySQL, Flyway nạp lại khi ứng dụng khởi động
```

## Tài liệu kế hoạch

| Tệp | Nội dung |
|---|---|
| [De_cuong_DATN.docx](De_cuong_DATN.docx) | Đề cương đồ án (nguồn yêu cầu) |
| [docs/00-KE-HOACH-TONG-THE.md](docs/00-KE-HOACH-TONG-THE.md) | Phạm vi, tác nhân, ma trận chức năng, kiến trúc, sơ đồ URL, quy tắc nghiệp vụ, bảo mật, kiểm thử, rủi ro, bàn giao |
| [docs/01-MO-HINH-DU-LIEU.md](docs/01-MO-HINH-DU-LIEU.md) | ERD + DDL MySQL, tham số `setting`, dữ liệu khởi tạo, truy vấn chính |
| [docs/02-LO-TRINH.md](docs/02-LO-TRINH.md) | Lộ trình 9 giai đoạn bám mốc thời gian của đề cương, checklist từng bước, thứ tự cắt khi thiếu thời gian, checklist demo, quyết định cần chốt |
| [docs/03-QUY-TAC-CODE.md](docs/03-QUY-TAC-CODE.md) | Quy tắc code back-end, Thymeleaf, MySQL/Flyway |
| [docs/04-KE-HOACH-CHATBOT.md](docs/04-KE-HOACH-CHATBOT.md) | Chatbot gợi ý truyện: kịch bản, các hàm tra cứu, hậu kiểm chống bịa, lịch sử hội thoại, đường lui, đánh giá |
| [CLAUDE.md](CLAUDE.md) | Bản rút gọn quy tắc cho người và công cụ hỗ trợ code |