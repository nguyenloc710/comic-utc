# Lộ trình thực hiện (02/10/2026 – 18/12/2026)

> Đi kèm [00-KE-HOACH-TONG-THE.md](00-KE-HOACH-TONG-THE.md). Mốc thời gian **bám đúng bảng kế hoạch ở mục 8 của đề cương**.
> Mỗi giai đoạn có **đầu ra kiểm chứng được** — làm xong phải demo được, không chỉ "đã viết code".

## Bảng tổng quan

| GĐ | Thời gian | Mục đề cương | Kết quả demo được |
|---|---|---|---|
| 0 | 02/10 – 07/10 | 2. Phân tích thiết kế | Use case, biểu đồ tuần tự, ERD, giao diện mẫu |
| 1 | 08/10 – 12/10 | 3. Dựng project | `docker compose up` + app chạy, Flyway sạch, 3 layout hiển thị, "hello tool" Spring AI trả lời được |
| 2 | 13/10 – 20/10 | 3. Đăng ký/đăng nhập, phân quyền, thể loại | Đăng ký → đăng nhập; 3 vai trò vào đúng khu vực; admin CRUD thể loại |
| 3 | 21/10 – 28/10 | 4. Đọc truyện, tìm kiếm, theo dõi, bình luận, đánh giá | Khách đọc được cả hai loại truyện trên dữ liệu demo; độc giả theo dõi/bình luận/đánh giá/đọc tiếp |
| 4 | 29/10 – 13/11 | 5. Đăng ký tác giả, studio, đăng truyện và chương | Độc giả gửi yêu cầu → admin duyệt → đăng truyện tranh + truyện chữ, lưu nháp, hẹn giờ, người theo dõi nhận thông báo |
| 5 | 14/11 – 18/11 | 6. Trang quản trị | Kiểm duyệt nội dung, người dùng, bình luận, báo cáo vi phạm, thống kê |
| 6 | 19/11 – 23/11 | 7. Chatbot AI | Chat tiếng Việt → thẻ truyện có thật, hỏi nối tiếp được |
| 7 | 24/11 – 13/12 | 8. Kiểm thử, sửa lỗi, triển khai | Test xanh, Docker image, dữ liệu demo đầy đủ, responsive |
| 8 | 14/12 – 18/12 | 9. Báo cáo, slide | Báo cáo + slide + video demo |

> **Lệch thứ tự cần biết trước.** Đề cương xếp "đọc truyện" (GĐ 3) trước "tác giả đăng truyện" (GĐ 4) ⇒ GĐ 3 phải chạy trên **dữ liệu demo sinh bằng script**.
> Tương tự, "duyệt tác giả" thuộc mục quản trị (GĐ 5) nhưng GĐ 4 đã cần ⇒ màn duyệt yêu cầu làm luôn ở GĐ 4, GĐ 5 làm phần quản trị còn lại.

---

## Giai đoạn 0 — Phân tích thiết kế (02/10 – 07/10)

- [ ] `docs/diagrams/usecase.puml`: 4 tác nhân + hệ thống, gom theo ma trận ở [00 §2.2](00-KE-HOACH-TONG-THE.md).
- [ ] `docs/diagrams/erd.puml` đúng với [01](01-MO-HINH-DU-LIEU.md).
- [ ] Biểu đồ tuần tự: `seq-author-request.puml` (gửi → duyệt → cấp quyền → phiên nhận quyền), `seq-publish-chapter.puml` (đăng ngay / hẹn giờ → job → thông báo),
      `seq-read-chapter.puml` (kiểm tra công khai → đếm lượt xem → lưu tiến độ), `seq-chatbot.puml` (tin nhắn → LLM → gọi hàm → hậu kiểm → thẻ truyện).
- [ ] `class.puml`: lớp miền chính (`Story`, `Chapter`, `ChapterPage`, `AuthorRequest`, …) và các service một cửa.
- [ ] Đặc tả use case chi tiết cho 6 UC: đăng ký tác giả, duyệt tác giả, đăng chương truyện tranh, đọc chương, tìm kiếm & lọc, chat gợi ý truyện.
- [ ] Giao diện mẫu (HTML tĩnh + Bootstrap trong `docs/mockups/`): trang chủ, chi tiết truyện, đọc truyện tranh, đọc truyện chữ, studio soạn chương, khung chat.
- [ ] Chốt các quyết định mở ở cuối tài liệu này.

**Đầu ra:** bộ sơ đồ + mockup đưa được vào Chương 3 của báo cáo.

---

## Giai đoạn 1 — Dựng project (08/10 – 12/10)

### 1.1. Repo & hạ tầng
- [x] Cấu trúc `backend/`, `docs/`; `.gitignore` thêm `.idea/`, `target/`, `.env`, `uploads/`, `logs/`.
- [x] `docker-compose.yml`: `mysql:8.4` (volume named, `utf8mb4_unicode_ci`, `--innodb-ft-min-token-size=1`) + `adminer`; `.env.example`. Image qua mirror `public.ecr.aws` như dự án mẫu.
- [x] Kiểm chứng: MySQL (cổng **3308**) và Adminer (cổng **8082**) chạy, DB `comic_utc` kết nối được. Cổng lệch khỏi 3306/8081 vì máy phát triển đang chạy MySQL/Adminer của dự án khác.

### 1.2. Backend skeleton (lấy `pom.xml` của dự án mẫu làm gốc)
- [x] Giữ: Web, Thymeleaf (+ Layout Dialect, extras-springsecurity6), Security, Data JPA, Validation, MySQL, Flyway (+ `flyway-mysql`), Cache + Caffeine, Actuator, Lombok, MapStruct (đúng thứ tự processor), springdoc, Testcontainers MySQL, spring-security-test, JaCoCo.
- [x] Bỏ: Redis, Mail, Micrometer Prometheus, POI, `anthropic-java`, Font Awesome.
- [x] Thêm: `spring-ai-bom` + starter mô hình, `jsoup`, WebJars `bootstrap`, `bootstrap-icons`, `chart.js`, `quill`, `sortablejs`.
- [x] `application.yml` + profile `dev` / `prod` / `test` theo mẫu (`ddl-auto: validate`, `open-in-view: false`, `jdbc.time_zone: UTC`, multipart, session cookie).
- [x] Chuyển thể từ dự án mẫu (đổi package): `BaseEntity`, `AuditableEntity`, `ApiResponse`, `PageResponse`, `ApiException`, `ErrorCode`, `FieldValidationException`,
      `GlobalExceptionHandler`, `MessageService`, `Setting*`, `AuditService`, `ClockConfig`, `JpaConfig`, `CacheConfig`, `AsyncSchedulingConfig`, `FlashMessages`,
      `GlobalModelAttributes`, `ErrorPageController`, `ApiErrorResponder`, `*Constants`.
- [x] `StorageService` + `LocalStorageService` + `ImageValidator` (viết lại từ `FileStorageService` của mẫu: đổi whitelist sang ảnh, thêm đọc kích thước ảnh); `WebMvcConfig` map `/media/**`.
- [x] Kiểm chứng: `GET /actuator/health` = `UP`; `GET /khong-ton-tai` trả trang 404 Thymeleaf.

### 1.3. Schema & entity
- [x] `V1__init_schema.sql` (toàn bộ bảng ở [01 §2–6, §8](01-MO-HINH-DU-LIEU.md)), `V2__seed_master_data.sql` (thể loại, setting, admin).
- [x] Entity + enum + repository cơ bản cho mọi bảng; `Story`, `Chapter` có `@Version`; cột bộ đếm khai báo `updatable = false` (có test chứng minh lưu thực thể không ghi đè bộ đếm).
- [x] `db/demo/V100__demo_accounts.sql`: tài khoản `author1`, `reader1` (làm sớm hơn kế hoạch để có vai trò AUTHOR/USER mà thử layout; truyện mẫu vẫn ở GĐ 3).
- [x] Kiểm chứng: xóa volume → khởi động → Flyway chạy sạch, `validate` không lỗi.

### 1.4. Khung giao diện
- [x] `layout/site.html`, `layout/studio.html`, `layout/admin.html`; fragment `head`, `flash`, `pagination`, `user-menu`.
      Fragment `story-card` và `badges` dời sang GĐ 3.1: chúng phụ thuộc hình dạng DTO thẻ truyện, viết trước khi có DTO là đoán.
- [x] `static/css/app.css` (biến màu chủ đề đè lên Bootstrap), `static/js/app.js` (`App.fetchJson` gắn CSRF).
- [x] Kiểm chứng: 3 layout hiển thị đúng ở 500 / 1440 px (chụp bằng Edge headless; sidebar thành offcanvas dưới 992 px).
- [ ] Kiểm tra tay ở 360 px trên điện thoại thật hoặc DevTools — Edge headless không thu cửa sổ nhỏ hơn 500 px.
- Quy ước: mục menu chỉ được thêm vào layout khi trang đích đã tồn tại, nên navbar/sidebar hiện mới có rất ít mục.

### 1.5. Thử Spring AI sớm (khử rủi ro, ~1 giờ)
- [x] Chốt Spring AI **1.1.8** (dòng 1.1.x dành cho Boot 3.5; dòng 2.x cần Boot 4) + `spring-ai-starter-model-anthropic`; ứng dụng khởi động được cả khi chưa có khóa.
- [x] Mạng hiện tại **không chặn** nhà cung cấp nào: `api.anthropic.com`, `api.openai.com`, `generativelanguage.googleapis.com` đều trả lời (401/403 vì chưa gửi khóa).
- [x] Viết `SpringAiToolCallingSmokeTest`: gọi `ChatClient` với một `@Tool` giả, khẳng định mô hình gọi hàm và dùng kết quả hàm.
- [ ] **Chưa chạy được với LLM thật vì máy chưa có khóa API.** Chạy: `AI_API_KEY=... ./mvnw test -Dtest=SpringAiToolCallingSmokeTest` (không có biến này thì test tự bỏ qua).
- [ ] Test trên không đạt ⇒ đổi nhà cung cấp ngay từ bây giờ, không đợi tới GĐ 6.

---

## Giai đoạn 2 — Tài khoản, phân quyền, thể loại (13/10 – 20/10)

### 2.1. Bảo mật
- [ ] `SecurityConfig`: `permitAll` cho phần công khai (`/`, `/stories/**`, `/genres/**`, `/rankings`, `/media/**`, `/login`, `/register`, static, webjars);
      `/me/**` + `/api/**` cần đăng nhập (trừ các GET công khai); `/studio/**` AUTHOR; `/admin/**`, actuator, swagger ADMIN; `RoleHierarchy` `AUTHOR > USER`, `ADMIN > USER`; header bảo mật.
- [ ] `AppUserDetailsService`, `AppUserPrincipal` (id, username, displayName, role), `SecurityUtils`.
- [ ] `AuthorityRefreshFilter` + cache vai trò/trạng thái (evict khi đổi) — xem [00 §4.1](00-KE-HOACH-TONG-THE.md).
- [ ] `LoginAttemptService`: đếm sai, khóa tạm theo `setting`.

### 2.2. Đăng ký, đăng nhập, hồ sơ
- [ ] `RegistrationService.register`: kiểm tra trùng username/email, chính sách mật khẩu, BCrypt; đăng ký xong đăng nhập luôn.
- [ ] Trang `auth/register`, `auth/login` (thông báo theo mã: sai mật khẩu / bị khóa tạm / bị cấm), `me/profile` (tên hiển thị, giới thiệu, avatar), đổi mật khẩu.

### 2.3. Thể loại & người dùng (ADMIN)
- [ ] `AdminGenreController`: CRUD thể loại (tên, slug tự sinh, mô tả, thứ tự, ẩn/hiện); không xóa thể loại đang có truyện (chỉ ẩn); evict cache.
- [ ] `AdminUserController` bản đầu: danh sách + lọc + khóa/mở khóa (đủ để test `AuthorityRefreshFilter`).
- [ ] Test tích hợp: khách vào `/me/library` ⇒ 302 `/login`; USER vào `/studio`, `/admin` ⇒ 403; sai N lần ⇒ khóa; khóa tài khoản đang đăng nhập ⇒ request kế tiếp bị đá.

**Đầu ra:** đăng ký/đăng nhập/phân quyền hoàn chỉnh; admin quản lý được thể loại.

---

## Giai đoạn 3 — Đọc truyện & tương tác (21/10 – 28/10) ⭐ TRỌNG TÂM 1

### 3.0. Dữ liệu demo (làm đầu tiên)
- [ ] `backend/scripts/generate-demo-data.mjs` → `db/demo/V101__demo_content.sql` + bộ ảnh giữ chỗ (bìa, trang truyện) tự tạo. Nội dung theo [01 §9](01-MO-HINH-DU-LIEU.md). (`V100` đã dùng cho tài khoản demo.)

### 3.1. Danh mục công khai
- [ ] `StorySpecification` (`publiclyVisible`, keyword, genres, type, status, khoảng số chương) + `StoryCatalogQueryService.searchStories(filter, pageable)` — **thiết kế tham số đủ cho cả chatbot** ([04 §4](04-KE-HOACH-CHATBOT.md)).
- [ ] Trang chủ (mới cập nhật, nổi bật tuần, truyện mới, hoàn thành, "Đọc tiếp"); `/stories` (ô tìm + bộ lọc + sắp xếp + phân trang trên query string); `/genres/{slug}`; `/rankings` (tab ngày/tuần/tháng/theo dõi/điểm).
- [ ] Fragment `story-card`, `badges` (loại/trạng thái truyện) dựng theo `StoryCardResponse`; thêm ô tìm kiếm và các mục Truyện / Xếp hạng vào navbar của `layout/site`.
- [ ] Trang chi tiết truyện: bìa, mô tả, thể loại, tác giả (bút danh), số liệu, danh sách chương, nút Đọc từ đầu / Đọc tiếp / Theo dõi, đánh giá, bình luận.

### 3.2. Trang đọc
- [ ] `ChapterReaderService.getChapterForReading(slug, chapterNo, viewer)`: kiểm tra công khai (tác giả/admin xem được bản nháp/ẩn của phạm vi mình), trả chương + chương trước/sau.
- [ ] `chapter/read-comic` (ảnh xếp dọc, lazy-load, giữ tỉ lệ khung), `chapter/read-novel` (HTML đã làm sạch, cỡ chữ, nền sáng/tối); thanh điều hướng chương; phím ← →.
- [ ] `ChapterViewedEvent` → `ViewCountService` (khử trùng lặp, cộng bộ đếm, upsert `story_view_daily`).
- [ ] `ReadingHistoryService.recordProgress` (upsert) khi độc giả mở chương.

### 3.3. Tương tác
- [ ] `FollowService.toggleFollow`, `RatingService.rate`, `CommentService` (thêm, trả lời một cấp, tự xóa, cooldown) + `/api/**` tương ứng + JS cập nhật không tải lại trang.
- [ ] `/me/library`, `/me/history`.
- [ ] `RankingService` (cache 10 phút).
- [ ] Test: URL chương nháp/ẩn/của truyện ẩn ⇒ 404 với khách; đánh giá lại ⇒ `rating_sum` đổi đúng chênh lệch; theo dõi hai lần liên tiếp không lệch `follow_count`.

**Đầu ra:** phía độc giả chạy đầy đủ trên dữ liệu demo.

---

## Giai đoạn 4 — Tác giả (29/10 – 13/11) ⭐ TRỌNG TÂM 2

### 4.1. Đăng ký & duyệt tác giả (29/10 – 01/11)
- [ ] `AuthorRequestService.submit / approve / reject` theo [00 §4.1](00-KE-HOACH-TONG-THE.md); trang `/me/author-request` (form hoặc trạng thái + lý do từ chối).
- [ ] `/admin/author-requests`: danh sách chờ, xem chi tiết, duyệt / từ chối kèm lý do.
- [ ] `NotificationListener` + chuông thông báo + `/me/notifications` (làm ở đây vì duyệt tác giả là nơi đầu tiên cần).
- [ ] Kiểm chứng: độc giả đang đăng nhập được duyệt ⇒ tải lại trang là thấy menu Studio, không phải đăng nhập lại.

### 4.2. Quản lý truyện (02/11 – 05/11)
- [ ] `StudioStoryService` create / update / delete (mềm) / publish; `StoryAccessPolicy`; `SlugUtils`; upload bìa.
- [ ] `/studio/stories` (danh sách + trạng thái + cờ bị ẩn kèm lý do), form tạo/sửa (loại, bìa, mô tả, thể loại nhiều lựa chọn, trạng thái).

### 4.3. Chương (06/11 – 11/11)
- [ ] `ChapterStatus.canTransitionTo` + unit test; `ChapterPublishService` (một cửa); `ChapterPublishJob` + `job_run`.
- [ ] Truyện tranh: `ChapterPageService` upload từng ảnh, sắp xếp tự nhiên, kéo thả đổi thứ tự, xóa ảnh; `studio/chapter-comic-editor` (hàng đợi upload có tiến độ, SortableJS).
- [ ] Truyện chữ: `studio/chapter-novel-editor` (Quill) + `HtmlSanitizer` lúc lưu + `word_count`.
- [ ] Nút Lưu nháp / Hẹn giờ (chọn ngày giờ VN) / Đăng ngay / Hủy hẹn; danh sách chương có nhãn trạng thái.
- [ ] `ChapterPublishedEvent` → thông báo người theo dõi (`INSERT ... SELECT`).
- [ ] Test: job chạy 3 lần liên tiếp ⇒ chương đăng đúng 1 lần, 1 thông báo/người; tác giả A sửa chương của B ⇒ 404; payload XSS trong nội dung ⇒ bị loại.

### 4.4. Thống kê tác giả (12/11 – 13/11)
- [ ] `AuthorStatsService`: tổng lượt xem / theo dõi / bình luận / điểm theo truyện; biểu đồ lượt xem 30 ngày (`story_view_daily`) bằng Chart.js; top chương.

**Đầu ra:** vòng đời đầy đủ từ đăng ký tác giả đến chương mới tới tay người theo dõi.

---

## Giai đoạn 5 — Quản trị (14/11 – 18/11)

- [ ] `StoryModerationService`: ẩn/hiện truyện, ẩn/hiện chương (qua `ChapterPublishService`) — bắt buộc lý do, thông báo tác giả, ghi audit.
- [ ] `/admin/stories` (lọc theo loại/hiển thị/tác giả, xem cả nội dung ẩn), `/admin/comments` (ẩn/hiện), `/admin/reports` (hàng đợi báo cáo → mở đối tượng → bỏ qua / ẩn nội dung).
- [ ] Nút "Báo cáo vi phạm" ở trang truyện, trang đọc, từng bình luận (`ReportService.submit`).
- [ ] `/admin/users` hoàn chỉnh: lọc, khóa/mở, đổi vai trò; không tự khóa mình, không khóa admin cuối cùng.
- [ ] `/admin` dashboard: KPI (người dùng, tác giả, truyện theo loại, chương, lượt xem hôm nay, yêu cầu chờ duyệt, báo cáo chờ xử lý) + biểu đồ (đăng ký mới & lượt xem 30 ngày, phân bố thể loại, top truyện).
- [ ] `/admin/settings` (sửa tham số, validate theo kiểu, xóa cache), `/admin/audit-logs`.

---

## Giai đoạn 6 — Chatbot AI (19/11 – 23/11) ⭐ TRỌNG TÂM 3

Chi tiết từng bước ở [04 §10](04-KE-HOACH-CHATBOT.md). Tóm tắt theo ngày:

- [ ] **19/11** — `V3__chatbot.sql`, entity; `StoryCatalogTools` (4 hàm) bọc `StoryCatalogQueryService`; test hàm chỉ trả truyện công khai.
- [ ] **20/11** — `prompts/chatbot-system.txt`; `ChatbotService.reply` (lịch sử + gọi `ChatClient` + nhận JSON có cấu trúc); `RecommendationValidator` (hậu kiểm).
- [ ] **21/11** — `/api/chat/**` + `chat-widget` + `chat.js` (thẻ truyện, đang gõ, lịch sử, gợi ý câu hỏi mẫu).
- [ ] **22/11** — Giới hạn theo ngày, đường lui tìm theo từ khóa, xử lý timeout/lỗi; `/admin/chatbot` (số tin, tỉ lệ rơi đường lui, phản hồi 👍/👎).
- [ ] **23/11** — Chạy bộ câu hỏi đánh giá, chỉnh prompt/mô tả hàm, ghi số liệu cho báo cáo.

---

## Giai đoạn 7 — Kiểm thử, sửa lỗi, triển khai (24/11 – 13/12)

- [ ] Bổ sung unit/integration test theo [00 §8](00-KE-HOACH-TONG-THE.md); coverage service ≥ 60% (JaCoCo).
- [ ] Soi N+1 (`show-sql`) ở trang chủ, danh sách, chi tiết truyện; `@EntityGraph`/projection; `EXPLAIN` truy vấn tìm kiếm và xếp hạng.
- [ ] Rà bảo mật: `th:utext` ngoài nội dung chương, CSRF bằng curl không token ⇒ 403, IDOR ở `/studio/**` và `/api/studio/**`, path traversal khi upload, tệp giả đuôi ảnh.
- [ ] Rà chuỗi viết cứng (grep tiếng Việt ngoài `messages.properties`, `prompts/` và comment).
- [ ] Responsive 360 / 768 / 1440 px toàn bộ trang; kiểm thử trên điện thoại thật.
- [ ] `backend/Dockerfile` nhiều tầng (theo mẫu) + compose prod (app + mysql + volume `uploads`); hướng dẫn cài đặt trong README.
- [ ] (Tùy chọn) `CloudinaryStorageService` + triển khai lên máy chủ/VPS; GitHub Actions build + test.
- [ ] Dữ liệu demo bản cuối; kịch bản demo chạy trơn từ máy sạch.

## Giai đoạn 8 — Báo cáo & slide (14/12 – 18/12)

- [ ] Xuất PNG sơ đồ; ảnh chụp màn hình theo vai trò; bảng kết quả kiểm thử; số liệu đánh giá chatbot.
- [ ] Báo cáo theo bố cục đề cương §7; slide; video demo dự phòng.

---

## Thứ tự cắt khi thiếu thời gian

Cắt từ trên xuống:

1. Cloudinary, triển khai VPS, GitHub Actions (giữ lưu cục bộ + Docker).
2. Swagger, trang audit log (giữ ghi audit), trang cài đặt (giữ `setting` sửa qua DB).
3. Xếp hạng theo ngày/tuần/tháng (giữ theo tổng lượt xem), trả lời bình luận (giữ bình luận phẳng).
4. Hàm chatbot phụ `findSimilarStories`, `getTrendingStories`, phản hồi 👍/👎 (giữ `searchStories` + `getStoryDetail`).
5. Khóa tạm khi sai mật khẩu, chế độ sáng/tối trang đọc.

**Tuyệt đối không cắt** (đều là cam kết trong đề cương): 4 vai trò và phân quyền; đọc cả truyện tranh lẫn truyện chữ; tìm kiếm & lọc; theo dõi, lịch sử, bình luận, đánh giá, thông báo chương mới;
quy trình đăng ký–duyệt tác giả; đăng chương có lưu nháp và hẹn giờ; kiểm duyệt ẩn nội dung, báo cáo vi phạm, thống kê; chatbot function calling trả thẻ truyện có thật và hỏi nối tiếp được.

---

## Checklist demo trước khi bảo vệ

- [ ] Máy sạch: `docker compose up -d` → `mvnw spring-boot:run` → có dữ liệu demo, đăng nhập được 3 vai trò.
- [ ] Kịch bản ~8 phút: khách tìm "tu tiên", lọc truyện chữ đã hoàn thành, đọc thử → đăng ký tài khoản → theo dõi, đánh giá, bình luận → gửi yêu cầu làm tác giả →
      admin duyệt → (không đăng nhập lại) vào Studio tạo truyện tranh, upload 10 ảnh, kéo đổi thứ tự, hẹn giờ 2 phút sau → tạo truyện chữ, đăng ngay →
      tới giờ chương tự đăng, người theo dõi nhận thông báo → mở chatbot: "truyện tu tiên, nữ chính mạnh mẽ, đã hoàn thành" → bấm thẻ truyện → hỏi tiếp "có truyện nào ngắn hơn không?" →
      độc giả báo cáo vi phạm → admin ẩn chương kèm lý do → tác giả thấy lý do → dashboard admin.
- [ ] Bảo mật: khách mở URL chương nháp ⇒ 404; tác giả A mở trang sửa chương của B ⇒ 404; dán `<script>` vào nội dung chương ⇒ không chạy.
- [ ] Chatbot: hỏi ngoài phạm vi ("viết hộ bài văn") ⇒ từ chối lịch sự; hỏi thể loại không có truyện ⇒ nói thật là không có, không bịa; rút mạng ⇒ rơi về tìm theo từ khóa.
- [ ] Chuẩn bị trả lời hội đồng: vì sao function calling thay vì nhét cả kho truyện vào prompt; chống bịa truyện bằng cách nào; vì sao session thay vì JWT; vì sao bộ đếm phi chuẩn hóa;
      vì sao làm sạch HTML lúc lưu; job hẹn giờ chạy trùng thì sao; phiên đang đăng nhập nhận quyền AUTHOR thế nào.

---

## Quyết định cần chốt (đang đi theo phương án mặc định)

| # | Câu hỏi | Mặc định trong kế hoạch | Ảnh hưởng nếu đổi |
|---|---|---|---|
| 1 | Nhà cung cấp LLM | Claude qua `spring-ai-starter-model-anthropic`, mô hình đặt bằng biến môi trường | Đổi starter + cấu hình; code không đổi ([04 §3](04-KE-HOACH-CHATBOT.md)) |
| 2 | Kiểm duyệt truyện | Hậu kiểm: tác giả đã duyệt đăng là hiện ngay, admin ẩn khi vi phạm | Tiền kiểm cần thêm trạng thái `PENDING_REVIEW` và hàng đợi duyệt (~2 ngày) |
| 3 | Lưu ảnh | Đĩa cục bộ; Cloudinary là tùy chọn ở GĐ 7 | Chỉ thêm một lớp cài đặt `StorageService` |
| 4 | Soạn truyện chữ | Quill → HTML làm sạch | Đổi sang text thuần thì bỏ được `HtmlSanitizer` nhưng mất định dạng |
| 5 | Package gốc / tên DB | `vn.edu.utc.comic` / `comic_utc` | Chỉ ảnh hưởng lúc dựng project |
