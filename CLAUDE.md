# CLAUDE.md

Website đọc và đăng tải truyện tranh, truyện chữ tích hợp chatbot AI gợi ý truyện — đồ án tốt nghiệp (ĐH GTVT). Nguồn yêu cầu: `De_cuong_DATN.docx`.
Stack: Java 21 · Spring Boot 3.5 (MVC, Security, Data JPA, Scheduler) · **Thymeleaf + Bootstrap 5 qua WebJars** · **MySQL 8.4** (Flyway) · đăng nhập **session/form login + CSRF**, vai trò `USER` / `AUTHOR` / `ADMIN` (+ khách vãng lai) · **Spring AI** function calling cho chatbot.

Kiến trúc và quy tắc code **kế thừa từ dự án mẫu** `C:\Users\locnh4\Desktop\LOC\it-support-management` (package-by-feature, common layer, MapStruct, Flyway, `setting` trong DB). Khi cần mẫu code cụ thể (`GlobalExceptionHandler`, `SettingService`, `SecurityConfig`, `FileStorageService`, `pom.xml`, `Dockerfile`, `docker-compose.yml`), mở dự án đó xem rồi chuyển thể — **không** copy phần SLA/ticket/RAG/Redis/mail.

Tài liệu: [docs/00 kế hoạch tổng thể](docs/00-KE-HOACH-TONG-THE.md) · [01 mô hình dữ liệu](docs/01-MO-HINH-DU-LIEU.md) · [02 lộ trình](docs/02-LO-TRINH.md) · [03 quy tắc code](docs/03-QUY-TAC-CODE.md) · [04 kế hoạch chatbot](docs/04-KE-HOACH-CHATBOT.md)

---

## Quy tắc bắt buộc khi viết code

Đầy đủ ở [docs/03-QUY-TAC-CODE.md](docs/03-QUY-TAC-CODE.md). Bản rút gọn:

1. **Không hardcode.** Cố định → `common/constant/*Constants.java`; đổi khi vận hành (dung lượng ảnh, giãn cách bình luận, hạn mức chat) → bảng `setting` qua `SettingService`; theo môi trường (thư mục lưu ảnh, tên mô hình LLM) → `application.yml` + `@ConfigurationProperties`; tập đóng → `enum`; tên view/redirect → `ViewConstants`.
2. **Không chuỗi hiển thị trong Java lẫn template.** Service ném `ApiException(ErrorCode.X)`; `GlobalExceptionHandler` là nơi duy nhất dịch (JSON cho `/api/**`, trang lỗi cho phần còn lại). Template dùng `#{key}` từ `i18n/messages.properties`; enum qua `enum.<Enum>.<VALUE>`. Prompt cho LLM nằm ở `resources/prompts/`.
3. **Đặt tên tiếng Anh** (class, method, biến, bảng, cột, URL, khóa messages, id/class HTML). Tiền tố: `get`/`find`/`search`/`create|update|delete`/`change`/`toggle`/`is|has|can`/`validate`/`build`/`calculate`/`handle`/`record`.
4. **Comment & Javadoc tiếng Việt**, giải thích *tại sao*. Javadoc bắt buộc cho method public của service/controller.
5. **Hai loại controller**: `@Controller` trả view, POST theo PRG (flash + redirect); `@RestController` chỉ dưới `/api/**`, có Swagger.
6. **MapStruct cho mọi ánh xạ**, `unmappedTargetPolicy = ERROR`; cấm hàm map tay. Thứ tự processor: lombok → lombok-mapstruct-binding → mapstruct.
7. **Chia hàm**: method ≤ 30 dòng, Cognitive Complexity ≤ 15, lồng ≤ 3, tham số ≤ 5; return sớm.
8. **Sạch SonarQube**: không `System.out`, không nối chuỗi trong log, không nuốt ngoại lệ, không `RuntimeException` trần, không field injection, không trả `null` cho collection, DTO là `record`.
9. **Tầng nào việc nấy**: Controller DTO ↔ service; Service nghiệp vụ + `@Transactional`; Repository truy vấn; Mapper ánh xạ; Job và Tools chỉ gọi service. **Không đưa Entity vào Model/template.**
10. **Thời gian**: DB `DATETIME(6)` UTC, entity `Instant`, inject `Clock` (không `Instant.now()` trong service/job); hiển thị và nhập giờ hẹn theo `Asia/Ho_Chi_Minh`.

## Nghiệp vụ dễ sai — luôn giữ đúng

- **Điều kiện công khai có một định nghĩa** (`StorySpecification.publiclyVisible()`: truyện `PUBLISHED`, chưa xóa mềm; chương `PUBLISHED`). **Mọi** truy vấn phía người đọc và **mọi hàm của chatbot** đi qua `StoryCatalogQueryService` / `ChapterReaderService`. Nội dung không được phép xem trả **404**, không 403.
- **Máy trạng thái chương** khai báo trong `ChapterStatus.canTransitionTo`; xuất bản/ẩn/hiện **chỉ** qua `ChapterPublishService` — nơi duy nhất sửa `story.chapter_count`, `last_chapter_at` và phát `ChapterPublishedEvent`.
- **Hẹn giờ đăng**: `ChapterPublishJob` mỗi phút, xuất bản bằng UPDATE có điều kiện `WHERE status = 'SCHEDULED'`, mỗi chương một transaction, **idempotent**.
- **Bộ đếm** (`view_count`, `follow_count`, `rating_sum/count`, `comment_count`) cập nhật bằng `UPDATE … SET x = x + :delta`, không đọc-rồi-ghi.
- **Quyền theo dữ liệu**: tác giả chỉ sửa truyện/chương của mình, kiểm tra ở `StoryAccessPolicy` trong service; `sec:authorize` chỉ để ẩn menu.
- **Cấp quyền AUTHOR**: `AuthorRequestService.approve` đổi vai trò + tạo `author_profile` + evict cache quyền; `AuthorityRefreshFilter` cho phiên đang đăng nhập nhận quyền mới. Mỗi người tối đa một yêu cầu `PENDING` (UNIQUE trên cột sinh).
- **HTML chương truyện chữ** làm sạch bằng `HtmlSanitizer` (jsoup whitelist) **lúc lưu**; đó là chỗ duy nhất được `th:utext`. Bình luận, mô tả là text thuần.
- **Ảnh**: `ImageValidator` xác định định dạng từ nội dung tệp (không tin đuôi/MIME), tên UUID, DB giữ khóa tương đối, URL dựng qua `StorageService.resolveUrl`; upload từng ảnh một request, thứ tự do service ghi lại 1..n.
- **Thông báo** qua `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`; chương mới phát cho người theo dõi bằng một câu `INSERT … SELECT`.
- **Chatbot**: hàm chỉ đọc; thẻ truyện dựng từ DB theo id đã qua `RecommendationValidator` (id phải nằm trong kết quả hàm của hội thoại và còn công khai); `userId` lấy từ `SecurityContext`, không nhận từ mô hình; LLM lỗi ⇒ `KeywordFallbackResponder`; khóa API chỉ qua biến môi trường.

## Thymeleaf

- Layout Dialect với ba layout: `layout/site.html` (công khai + độc giả), `layout/studio.html` (tác giả), `layout/admin.html`; fragment tham số hóa cho story-card/badges/pagination/comment-list/chat-widget. Không copy-paste markup.
- Form: `th:object` + `th:field` + `th:errors` gắn lớp `*Form` (có setter, Bean Validation); `record` chỉ dùng cho response/filter; `th:action` để tự chèn CSRF.
- JS ở `static/js/`, `fetch()` qua `App.fetchJson()` gắn `X-CSRF-TOKEN` từ meta; chèn dữ liệu bằng `textContent`. Bootstrap, Bootstrap Icons, Chart.js, Quill, SortableJS qua WebJars, không CDN.

## Lệnh thường dùng

```bash
docker compose up -d                                   # mysql (3308), adminer (8082)
cd backend && ./mvnw spring-boot:run                   # profile mặc định là dev; http://localhost:8080
./mvnw verify                                          # test (Testcontainers MySQL, cần Docker đang chạy) + JaCoCo
AI_API_KEY=... ./mvnw test -Dtest=SpringAiToolCallingSmokeTest   # gọi LLM thật; không có khóa thì tự bỏ qua
```

Tài khoản dev: `admin` / `Admin@123` · `author1`, `reader1` / `Demo@123`. Bộ đếm (`*_count`, `rating_sum`) là cột `updatable = false`: chỉ đổi bằng câu UPDATE cộng dồn, đừng `setXxxCount` rồi `save`.
Mục menu chỉ thêm vào layout khi trang đích đã tồn tại.
