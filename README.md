# comic-utc

Website đọc và đăng tải truyện tranh, truyện chữ tích hợp chatbot AI gợi ý truyện — đồ án tốt nghiệp, Khoa CNTT, Trường ĐH GTVT.

- **Front-end:** Thymeleaf (+ Layout Dialect), HTML/CSS, Bootstrap 5, JavaScript
- **Back-end:** Java 21 + Spring Boot 3.5 (Spring MVC, Spring Security, Spring Data JPA / Hibernate)
- **CSDL:** MySQL 8.4 (Flyway quản lý schema)
- **Chatbot:** Spring AI + function calling
- **Vai trò:** khách vãng lai, độc giả (`USER`), tác giả (`AUTHOR`), quản trị viên (`ADMIN`)

> Trạng thái: xong **giai đoạn 3** — phía độc giả chạy đầy đủ trên dữ liệu demo: trang chủ, tìm kiếm và lọc truyện, bảng xếp hạng, trang chi tiết, trang đọc truyện tranh/truyện chữ (đếm lượt xem, lưu tiến độ), theo dõi, đánh giá, bình luận, tủ truyện, lịch sử đọc. Trước đó: tài khoản, phân quyền, quản trị người dùng và thể loại. Tác giả đăng truyện bắt đầu từ giai đoạn 4. Tiến độ chi tiết: [docs/02-LO-TRINH.md](docs/02-LO-TRINH.md).

## Chạy dự án

Yêu cầu: **JDK 21**, **Docker Desktop** (đang chạy), Git. Không cần cài Maven: dùng `mvnw` đi kèm.

```bash
docker compose up -d                     # MySQL (3308), Adminer (8082); chờ ~30s cho MySQL healthy
cd backend && ./mvnw spring-boot:run     # Windows CMD/PowerShell: mvnw.cmd spring-boot:run
```

Mở http://localhost:8080. Profile mặc định là `dev`: Flyway tạo lược đồ (`db/migration`) rồi nạp tài khoản và 64 truyện demo (`db/demo`).
`.env` là tùy chọn (compose và ứng dụng đã có giá trị mặc định khớp nhau); chỉ cần `cp .env.example .env` khi muốn đổi cổng, mật khẩu DB hoặc đặt khóa API cho chatbot.

| Tài khoản | Mật khẩu | Vai trò | Vào được |
|---|---|---|---|
| `admin` | `Admin@123` | Quản trị viên | `/admin` (người dùng, thể loại) |
| `author1` … `author3` | `Demo@123` | Tác giả | `/studio`; xem trước được truyện nháp/bị ẩn của chính mình |
| `reader1` … `reader15` | `Demo@123` | Độc giả | theo dõi, đánh giá, bình luận, `/me/library`, `/me/history`, `/me/profile` |

Hoặc tự tạo tài khoản độc giả ở `/register`.

Tài khoản `admin` nằm trong dữ liệu khởi tạo của mọi môi trường — **phải đổi mật khẩu trước khi triển khai thật**.

| Địa chỉ | Nội dung |
|---|---|
| http://localhost:8080 | Trang chủ |
| http://localhost:8080/stories · `/genres/{slug}` · `/rankings` | Tìm kiếm và lọc truyện · truyện theo thể loại · bảng xếp hạng |
| http://localhost:8080/stories/{slug} · `…/chapters/{số}` | Chi tiết truyện · trang đọc |
| http://localhost:8080/me/library · `/me/history` | Tủ truyện đang theo dõi · lịch sử đọc |
| http://localhost:8080/login · `/register` | Đăng nhập · đăng ký |
| http://localhost:8080/me/profile | Hồ sơ cá nhân, đổi mật khẩu |
| http://localhost:8080/studio | Khu vực tác giả |
| http://localhost:8080/admin | Quản trị |
| http://localhost:8080/swagger-ui.html | Tài liệu API JSON (`/api/**`, cần quyền ADMIN) |
| http://localhost:8080/actuator/health | Kiểm tra tình trạng hệ thống |
| http://localhost:8082 | Adminer — xem CSDL (server `mysql`, user `comic`, mật khẩu `comic_local_pwd`, DB `comic_utc`) |

### Kiểm thử

```bash
cd backend && ./mvnw verify              # unit test + test tích hợp (Testcontainers MySQL) + báo cáo JaCoCo
```

Test gọi LLM thật (`SpringAiToolCallingSmokeTest`) tự bỏ qua khi chưa có khóa; để chạy: đặt biến môi trường `AI_API_KEY` rồi `./mvnw test -Dtest=SpringAiToolCallingSmokeTest`.

Dữ liệu demo sinh bằng `node backend/scripts/generate-demo-data.mjs` (ghi đè `db/demo/V101__demo_content.sql` và ảnh trong `resources/demo-media`). Tệp SQL đã nằm sẵn trong repo; chỉ chạy lại script khi muốn đổi nội dung demo, và khi đó phải nạp lại dữ liệu từ đầu vì Flyway đã ghi nhận tệp cũ.

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