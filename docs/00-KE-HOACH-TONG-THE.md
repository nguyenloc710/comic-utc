# Kế hoạch tổng thể — Website đọc và đăng tải truyện tranh, truyện chữ tích hợp chatbot AI

> Tài liệu chủ đạo. Xem thêm: [01-MO-HINH-DU-LIEU.md](01-MO-HINH-DU-LIEU.md) · [02-LO-TRINH.md](02-LO-TRINH.md) · [03-QUY-TAC-CODE.md](03-QUY-TAC-CODE.md) · [04-KE-HOACH-CHATBOT.md](04-KE-HOACH-CHATBOT.md)
>
> Nguồn yêu cầu: `De_cuong_DATN.docx` (đề cương ngày 01/10/2026, SV Nguyễn Văn Thái, GVHD Phạm Thanh Hà).
> Kiến trúc back-end, quy tắc code và cách tổ chức tài liệu **kế thừa từ dự án mẫu `C:\Users\locnh4\Desktop\LOC\it-support-management`**
> (package-by-feature, `ApiException`/`ErrorCode`/`GlobalExceptionHandler`, MapStruct, Flyway, `setting` trong DB, Thymeleaf Layout Dialect,
> form login + session + CSRF, Javadoc tiếng Việt). Phần khác biệt so với mẫu nêu ở §1.2.

---

## 1. Tóm tắt đề tài

| Mục | Nội dung |
|---|---|
| Tên | Xây dựng website đọc và đăng tải truyện tranh, truyện chữ tích hợp chatbot AI gợi ý truyện, sử dụng Java Spring Boot MVC |
| Bài toán | Độc giả đọc truyện tranh (ảnh theo chương) và truyện chữ (văn bản theo chương); người dùng đăng ký làm tác giả (admin duyệt) để tự đăng truyện; chatbot AI gợi ý truyện **có thật trong kho** theo mô tả bằng tiếng Việt tự nhiên |
| Người dùng | **Khách vãng lai**, **Độc giả** (`USER`), **Tác giả** (`AUTHOR`), **Quản trị viên** (`ADMIN`) |
| Nền tảng | Web server-side rendering, responsive (máy tính + điện thoại). Không có app di động, không thanh toán/truyện trả phí |
| Front-end | Thymeleaf 3 (+ Layout Dialect), HTML/CSS, **Bootstrap 5**, JavaScript (AJAX cho khung chat, theo dõi, bình luận, upload ảnh) |
| Back-end | Java 21 (đề cương yêu cầu 17+), **Spring Boot 3.5** (Spring MVC, Spring Security, Spring Data JPA / Hibernate, Validation, Scheduler) |
| CSDL | **MySQL 8.4** (Flyway quản lý schema, Hibernate chỉ `validate`) |
| Xác thực | Spring Security form login + HTTP session, CSRF bật, BCrypt |
| Lưu ảnh | Đĩa cục bộ (mặc định) hoặc Cloudinary, chọn bằng cấu hình sau một interface `StorageService` |
| Chatbot | **Spring AI** `ChatClient` + function calling (`@Tool`), nhà cung cấp LLM chọn bằng cấu hình |

### 1.1. Ràng buộc kiến trúc phát sinh từ đề cương

1. **Thymeleaf ⇒ server-side rendering.** `@Controller` trả view; chỉ phần cần cập nhật không tải lại trang (chat, theo dõi, đánh giá,
   bình luận, chuông thông báo, upload/sắp xếp ảnh chương, dữ liệu biểu đồ) mới đi qua `@RestController` dưới `/api/**`.
   Không có SPA ⇒ không JWT, không CORS; dùng session + CSRF.
2. **Khách vãng lai đọc được truyện** ⇒ khác dự án mẫu (mẫu bắt đăng nhập toàn bộ): phần công khai `permitAll`, và **mọi truy vấn công khai
   phải lọc theo trạng thái hiển thị** (truyện `PUBLISHED`, chương `PUBLISHED`) ở tầng service/repository — không dựa vào template.
3. **Hai loại nội dung chung một mô hình**: `story.type` ∈ `COMIC` / `NOVEL`; chương truyện tranh là danh sách ảnh có thứ tự (`chapter_page`),
   chương truyện chữ là HTML đã làm sạch (`chapter_content`). Loại truyện **không đổi được** sau khi đã có chương.
4. **Quyền AUTHOR được cấp thêm lúc đang đăng nhập** ⇒ phiên hiện tại phải nhận quyền mới mà không bắt đăng nhập lại (xem §4.1).
5. **Phân quyền 2 lớp**: lớp URL (`/me/**` USER, `/studio/**` AUTHOR, `/admin/**` ADMIN, `/api/**` đã đăng nhập; **mọi đường dẫn khác mặc định cho qua**
   vì là trang công khai — nên trang nào cần đăng nhập bắt buộc phải nằm dưới một trong các tiền tố này) và lớp dữ liệu (tác giả chỉ sửa truyện/chương **của mình**)
   kiểm tra ở service (`StoryAccessPolicy`), không kiểm tra ở controller hay template.
6. **Hẹn giờ đăng chương** ⇒ job `@Scheduled` mỗi phút; xuất bản chương **chỉ đi qua một cửa** (`ChapterPublishService`) để cập nhật bộ đếm,
   mốc chương mới và phát thông báo cho người theo dõi — job và nút "Đăng ngay" dùng chung cửa này.
7. **Chatbot không được bịa truyện** ⇒ mô hình chỉ biết kho truyện qua các hàm tra cứu; thẻ truyện hiển thị được **dựng từ DB theo id**,
   id nào không xuất hiện trong kết quả hàm của hội thoại thì bị loại (hậu kiểm). Chi tiết ở [04](04-KE-HOACH-CHATBOT.md).
8. **Nội dung do người dùng tạo** (mô tả truyện, văn bản chương, bình luận) ⇒ XSS và prompt injection là rủi ro thật: HTML chương được làm sạch
   bằng whitelist lúc lưu; bình luận là text thuần; mô tả truyện đưa vào LLM được coi là dữ liệu, không phải chỉ dẫn.

### 1.2. Khác biệt so với dự án mẫu

| Hạng mục | `it-support-management` | `comic-utc` | Lý do |
|---|---|---|---|
| CSS | CSS tự viết, cấm Bootstrap | **Bootstrap 5 qua WebJars** + `app.css` nhỏ cho chủ đề | Đề cương ghi rõ Bootstrap 5 |
| Truy cập | Mọi trang đều phải đăng nhập | Trang đọc công khai, có khách vãng lai | Đề cương §4.2 |
| Đăng ký | Admin tạo tài khoản | **Tự đăng ký** | Website cộng đồng |
| Vai trò | Một vai trò cố định/người | Một vai trò, **nâng USER → AUTHOR khi được duyệt** | Đề cương §4.3 |
| Tệp | Đính kèm riêng tư, tải qua controller kiểm quyền | Ảnh bìa/ảnh chương **công khai**, phục vụ tĩnh qua `/media/**`, tên UUID | Khách cũng đọc được; cần nhanh và cache được |
| AI | RAG tự dựng + `anthropic-java` | **Spring AI + function calling** | Đề cương §5 chỉ định Spring AI |
| Bỏ hẳn | SLA, hàng đợi, Redis/Qdrant, Prometheus, Excel, mail | — | Không thuộc phạm vi đề cương |

---

## 2. Phân tích

### 2.1. Tác nhân

| Tác nhân | Mô tả | Quyền cốt lõi |
|---|---|---|
| **Khách vãng lai** | Chưa đăng nhập | Trang chủ, bảng xếp hạng, tìm kiếm/lọc, xem chi tiết truyện, đọc chương, đăng ký/đăng nhập |
| **Độc giả** (`USER`) | Thành viên | Như khách + hồ sơ, theo dõi, lịch sử đọc/đọc tiếp, bình luận, đánh giá sao, thông báo chương mới, chat với chatbot, báo cáo vi phạm, gửi yêu cầu làm tác giả |
| **Tác giả** (`AUTHOR`) | Độc giả đã được duyệt | Như độc giả + CRUD truyện của mình, đăng chương (ảnh/văn bản), lưu nháp, hẹn giờ, thống kê |
| **Quản trị viên** (`ADMIN`) | Quản trị | Duyệt yêu cầu tác giả, ẩn/hiện truyện & chương, quản lý người dùng/thể loại/bình luận/báo cáo, thống kê, tham số hệ thống |
| **Hệ thống** (scheduler) | Job định kỳ | Đăng chương hẹn giờ, dọn hội thoại chatbot cũ |

`RoleHierarchy`: `AUTHOR > USER`, `ADMIN > USER`. Admin **không** tự động là tác giả (không có bút danh, không vào `/studio`).

### 2.2. Ma trận chức năng theo vai trò (bám đề cương §4.2)

| Nhóm chức năng | Khách | Độc giả | Tác giả | Admin |
|---|---|---|---|---|
| Trang chủ, bảng xếp hạng, tìm kiếm & lọc (thể loại / trạng thái / loại truyện) | ✔ | ✔ | ✔ | ✔ |
| Đọc truyện tranh / truyện chữ | ✔ | ✔ + lưu tiến độ | ✔ | ✔ + xem cả nội dung đang ẩn |
| Đăng ký / đăng nhập / hồ sơ / đổi mật khẩu | đăng ký, đăng nhập | ✔ | ✔ | ✔ |
| Theo dõi truyện, lịch sử đọc, đọc tiếp | – | ✔ | ✔ | – |
| Bình luận, đánh giá sao | xem | ✔ | ✔ (không tự đánh giá truyện mình) | ẩn/xóa bình luận |
| Thông báo chương mới | – | ✔ | ✔ | – |
| Chatbot gợi ý truyện | – | ✔ | ✔ | xem thống kê, cấu hình |
| Báo cáo vi phạm | – | gửi | gửi | xử lý |
| Yêu cầu làm tác giả | – | gửi, xem kết quả | (đã duyệt) | duyệt / từ chối kèm lý do |
| Truyện: tạo / sửa / xóa | – | – | ✔ của mình | ẩn / hiện bất kỳ |
| Chương: đăng, lưu nháp, hẹn giờ | – | – | ✔ của mình | ẩn / hiện bất kỳ |
| Thống kê | – | – | lượt xem, theo dõi, bình luận theo truyện | tổng quan hệ thống |
| Người dùng, thể loại, tham số | – | – | – | ✔ |

---

## 3. Kiến trúc hệ thống

### 3.1. Sơ đồ tổng quan

```
   Trình duyệt (PC / mobile)
          │  HTML (Thymeleaf + Bootstrap 5) · fetch() JSON cho chat / theo dõi / bình luận / upload
          ▼
 ┌─────────────────────────────────────────────────────────────────────────┐
 │  Spring Boot 3.5 (một ứng dụng, cổng 8080)                              │
 │  ┌──────────────┐ ┌────────────────┐ ┌──────────────────────────────┐   │
 │  │ @Controller  │ │ @RestController│ │ Spring Security              │   │
 │  │ (Thymeleaf)  │ │ (/api/**)      │ │ form login · session · CSRF  │   │
 │  └──────┬───────┘ └───────┬────────┘ └──────────────────────────────┘   │
 │         ▼                 ▼                                             │
 │  ┌──────────────────────────────────────────────┐  ┌────────────────┐   │
 │  │ Service (nghiệp vụ, @Transactional)          │  │ @Scheduled     │   │
 │  │ Story · Chapter · ChapterPublish · Author    │  │ ChapterPublish │   │
 │  │ Interaction · Notification · Stats · Chatbot │  │ Job (1 phút)   │   │
 │  └───────┬──────────────────────────┬───────────┘  └────────────────┘   │
 │          ▼                          ▼                                   │
 │  Repository (JPA + Specification)   Spring AI ChatClient + @Tool        │
 │  MapStruct · Flyway                 (tool gọi ngược lại Service)        │
 └──────┬──────────────────┬─────────────────────────┬─────────────────────┘
        ▼                  ▼                         ▼
 ┌────────────┐   ┌──────────────────────┐   ┌───────────────────────┐
 │ MySQL 8.4  │   │ Ảnh: đĩa cục bộ      │   │ API mô hình ngôn ngữ  │
 │ (Flyway)   │   │ hoặc Cloudinary      │   │ (Claude/OpenAI/Gemini)│
 └────────────┘   └──────────────────────┘   └───────────────────────┘
```

### 3.2. Back-end — package-by-feature

```
vn.edu.utc.comic
├─ ComicApplication.java
├─ common/
│  ├─ config/      SecurityConfig, JpaConfig, WebMvcConfig (map /media/**), AsyncSchedulingConfig, CacheConfig,
│  │               ClockConfig, OpenApiConfig, StorageProperties, SiteProperties, ChatProperties
│  ├─ constant/    ApiConstants, ViewConstants, StoryConstants, SecurityConstants, CacheConstants,
│  │               DateTimeConstants, StorageConstants, MessageKeys
│  ├─ dto/         ApiResponse<T>, PageResponse<T>
│  ├─ entity/      BaseEntity, AuditableEntity
│  ├─ exception/   ApiException, ErrorCode, FieldValidationException, GlobalExceptionHandler
│  ├─ i18n/        MessageService
│  ├─ security/    AppUserDetailsService, AppUserPrincipal, SecurityUtils, AuthorityRefreshFilter,
│  │               LoginAttemptService, ApiErrorResponder
│  ├─ setting/     Setting, SettingKeys, SettingRepository, SettingService (cache Caffeine)
│  ├─ storage/     StorageService (interface), LocalStorageService, CloudinaryStorageService, ImageValidator, StoredFile
│  ├─ audit/       AuditLog, AuditAction, AuditService
│  ├─ web/         GlobalModelAttributes, FlashMessages, ErrorPageController
│  └─ util/        SlugUtils (bỏ dấu tiếng Việt), HtmlSanitizer (jsoup whitelist), NaturalOrderComparator
├─ auth/           AuthController (register, login, profile, change-password), RegistrationService
├─ user/           UserAccount, Role, UserStatus + AdminUserController, AdminUserService
├─ author/         AuthorRequest, AuthorProfile, AuthorRequestService (gửi / duyệt / từ chối),
│                  MeAuthorRequestController, AdminAuthorRequestController
├─ genre/          Genre, GenreService (cache), AdminGenreController
├─ story/
│  ├─ entity/      Story (+ story_genre)
│  ├─ enums/       StoryType, StoryStatus, StoryVisibility
│  ├─ controller/  HomeController, StoryBrowseController (/stories, /genres, /rankings), StoryDetailController,
│  │               StudioStoryController, AdminStoryController, StoryApiController
│  ├─ service/     StoryCatalogQueryService (MỌI truy vấn công khai — dùng chung cho web và chatbot),
│  │               StudioStoryService, StoryModerationService, StoryAccessPolicy
│  ├─ repository/  StoryRepository, StorySpecification
│  └─ mapper/      StoryMapper
├─ chapter/
│  ├─ entity/      Chapter, ChapterContent, ChapterPage
│  ├─ enums/       ChapterStatus (máy trạng thái)
│  ├─ controller/  ChapterReaderController, StudioChapterController, StudioChapterApiController (upload / sắp xếp ảnh)
│  ├─ service/     ChapterReaderService, StudioChapterService, ChapterPageService, ChapterPublishService (MỘT CỬA)
│  ├─ event/       ChapterPublishedEvent, ChapterViewedEvent
│  └─ job/         ChapterPublishJob
├─ interaction/    StoryFollow, ReadingHistory, StoryRating, Comment + FollowService, ReadingHistoryService,
│                  RatingService, CommentService, LibraryController (/me/library, /me/history), InteractionApiController
├─ report/         Report + ReportService, ReportApiController, AdminReportController
├─ notification/   Notification, NotificationService, NotificationListener (AFTER_COMMIT + @Async),
│                  NotificationApiController, NotificationController
├─ stats/          StoryViewDaily, ViewCountService, RankingService, AuthorStatsService, AdminDashboardService,
│                  StudioStatsController, AdminDashboardController, StatsApiController
├─ chatbot/        xem docs/04 — controller, service, tool, prompt, entity, dto
└─ system/         AdminSettingController, AdminAuditLogController
```

Nguyên tắc bắt buộc (chi tiết ở [03-QUY-TAC-CODE.md](03-QUY-TAC-CODE.md)):
- Controller chỉ nhận/trả DTO, không đưa Entity vào `Model`. Ánh xạ bằng **MapStruct**.
- Nghiệp vụ và `@Transactional` ở Service. Repository chỉ truy vấn; lọc động bằng `Specification`.
- Service ném `ApiException(ErrorCode.X)`; `GlobalExceptionHandler` là nơi duy nhất dịch thông báo (JSON cho `/api/**`, trang lỗi cho phần còn lại).
- Không hardcode: hằng số → `*Constants`; tham số vận hành → bảng `setting`; theo môi trường → `application.yml` + `@ConfigurationProperties`.
- Tên tiếng Anh, comment/Javadoc tiếng Việt.

### 3.3. Sơ đồ URL & template

| Khu vực | URL | Vai trò | Layout / template |
|---|---|---|---|
| Công khai | `/` · `/stories` (tìm & lọc) · `/genres/{slug}` · `/rankings` · `/stories/{slug}` · `/stories/{slug}/chapters/{no}` | tất cả | `layout/site` · `home`, `story/**`, `chapter/read-comic`, `chapter/read-novel` |
| Xác thực | `/login` · `/register` · `/logout` | khách | `auth/**` |
| Độc giả | `/me/profile` · `/me/library` (đang theo dõi) · `/me/history` · `/me/notifications` · `/me/author-request` | USER+ | `layout/site` · `me/**` |
| Tác giả | `/studio` · `/studio/stories` · `/studio/stories/new` · `/studio/stories/{id}/edit` · `/studio/stories/{id}/chapters` · `/studio/chapters/{id}/edit` · `/studio/stats` | AUTHOR | `layout/studio` · `studio/**` |
| Quản trị | `/admin` · `/admin/author-requests` · `/admin/stories` · `/admin/users` · `/admin/genres` · `/admin/comments` · `/admin/reports` · `/admin/chatbot` · `/admin/settings` · `/admin/audit-logs` | ADMIN | `layout/admin` · `admin/**` |
| JSON | `/api/chat/**` · `/api/stories/{id}/follow` · `/api/stories/{id}/rating` · `/api/comments` · `/api/reading-progress` · `/api/reports` · `/api/notifications/**` · `/api/studio/chapters/{id}/pages/**` · `/api/stats/**` | theo vai trò | – |
| Ảnh | `/media/**` (bìa, trang truyện, avatar; chỉ khi `storage.provider=LOCAL`) | tất cả | – |

Quy ước Thymeleaf:
- **Ba layout** qua Layout Dialect: `layout/site.html` (navbar: logo, ô tìm kiếm, menu thể loại, chuông, menu người dùng; nút chat nổi cho người đã đăng nhập),
  `layout/studio.html` và `layout/admin.html` (sidebar). Trang đọc chương dùng `layout/site` với thanh điều hướng chương dính đáy.
- Fragment tham số hóa: `story-card`, `pagination`, `flash`, `badges` (loại/trạng thái truyện, trạng thái chương), `rating-stars`, `comment-list`, `chat-widget`.
- Không chuỗi hiển thị viết cứng: mọi nhãn qua `#{key}`; enum qua `enum.<Enum>.<VALUE>`.
- Bootstrap 5, Bootstrap Icons, Chart.js, Quill (soạn truyện chữ), SortableJS (kéo thả sắp xếp ảnh) đều qua **WebJars** — không CDN để demo mất mạng vẫn chạy.
- JS chỉ ở `static/js/`; `fetch()` qua helper `App.fetchJson()` tự gắn `X-CSRF-TOKEN` từ thẻ meta.

### 3.4. Cấu trúc repo

```
comic-utc/
├─ backend/                 Maven project Spring Boot (chứa luôn Thymeleaf + static)
│  ├─ src/main/java/vn/edu/utc/comic
│  ├─ src/main/resources    application*.yml, db/migration, db/demo, i18n/messages.properties, prompts/, templates, static
│  ├─ src/test/java
│  └─ Dockerfile
├─ docs/                    bộ tài liệu này + diagrams/ (PlantUML: use case, ERD, sequence, lớp)
├─ docker-compose.yml       mysql, adminer
├─ .env.example
├─ CLAUDE.md                bản rút gọn quy tắc code
└─ README.md
```

---

## 4. Quy tắc nghiệp vụ cốt lõi

### 4.1. Tài khoản, vai trò và quy trình đăng ký tác giả (đề cương §4.3)

- Tự đăng ký bằng username + email + mật khẩu (BCrypt); vai trò mặc định `USER`. Chính sách mật khẩu đọc từ `setting`.
- **Gửi yêu cầu**: độc giả nhập bút danh (duy nhất toàn hệ thống), giới thiệu bản thân, loại truyện dự định (`COMIC` / `NOVEL` / `BOTH`).
  Mỗi người **tối đa một yêu cầu `PENDING`**; bị từ chối thì được gửi lại sau `author.request.cooldown.days`.
- **Duyệt**: `AuthorRequestService.approve` trong một transaction: yêu cầu → `APPROVED`, tạo `author_profile`, `user_account.role = AUTHOR`,
  ghi audit, phát `AuthorRequestReviewedEvent` → thông báo. **Từ chối bắt buộc có lý do**, hiển thị lại cho người gửi.
- **Phiên đang đăng nhập nhận quyền mới**: `AuthorityRefreshFilter` so vai trò/trạng thái trong `AppUserPrincipal` với giá trị hiện tại
  (đọc qua cache Caffeine, evict khi admin đổi vai trò/khóa) và dựng lại `Authentication` khi lệch. Cùng cơ chế này đá phiên của tài khoản vừa bị khóa.
- Admin khóa tài khoản (`BANNED`) ⇒ không đăng nhập được; truyện của tác giả bị khóa **không** tự ẩn (admin quyết định riêng).

### 4.2. Truyện

- `type` ∈ `COMIC` / `NOVEL` — chọn khi tạo, khóa sau khi có chương đầu tiên.
- `status` (tiến độ sáng tác) ∈ `ONGOING` / `COMPLETED` / `PAUSED` — tác giả tự đặt; đây là bộ lọc "trạng thái" phía độc giả.
- `visibility` (hiển thị) ∈ `DRAFT` → `PUBLISHED` ⇄ `HIDDEN`:
  - `DRAFT`: chỉ tác giả thấy. Tác giả bấm "Công khai" khi đã có bìa, mô tả, ≥ 1 thể loại.
  - `HIDDEN`: **chỉ admin** đặt/gỡ, bắt buộc lý do; tác giả thấy lý do trong studio và nhận thông báo; tác giả không tự gỡ ẩn được.
  - Mô hình **hậu kiểm**: tác giả đã được duyệt thì truyện công khai ngay, admin ẩn khi vi phạm (đúng chữ "kiểm duyệt/ẩn truyện, chương vi phạm" của đề cương).
- Xóa truyện là **xóa mềm** (`deleted_at`); tệp ảnh giữ lại. Slug sinh từ tên (bỏ dấu, `đ → d`), trùng thì thêm hậu tố `-2`, không đổi khi sửa tên.
- **Điều kiện hiển thị công khai** — một định nghĩa duy nhất, dùng lại ở mọi nơi (`StorySpecification.publiclyVisible()`):
  `visibility = PUBLISHED AND deleted_at IS NULL`; chương công khai: `status = PUBLISHED` và truyện cha công khai.
- Bộ đếm phi chuẩn hóa trên `story`: `chapter_count` (chương đã đăng), `view_count`, `follow_count`, `rating_sum`, `rating_count`, `comment_count`,
  `last_chapter_at` — cập nhật bằng `UPDATE ... SET x = x + 1` nguyên tử, không đọc-rồi-ghi.

### 4.3. Chương — máy trạng thái (điểm dễ sai nhất)

| Từ | Sang | Ai | Tác dụng phụ bắt buộc |
|---|---|---|---|
| *(tạo)* | `DRAFT` | Tác giả | Chưa tính vào `chapter_count`, không ai khác thấy |
| `DRAFT` | `SCHEDULED` | Tác giả | Bắt buộc `scheduled_at` ở tương lai; chương phải có nội dung (≥ 1 ảnh hoặc văn bản không rỗng) |
| `SCHEDULED` | `DRAFT` | Tác giả | Hủy hẹn giờ, xóa `scheduled_at` |
| `DRAFT` / `SCHEDULED` | `PUBLISHED` | Tác giả ("Đăng ngay") hoặc **job** khi tới giờ | `published_at = now`; `story.chapter_count + 1`; `story.last_chapter_at = now`; phát `ChapterPublishedEvent` → thông báo người theo dõi |
| `PUBLISHED` | `HIDDEN` | Admin (kèm lý do) | `story.chapter_count − 1`; thông báo tác giả |
| `HIDDEN` | `PUBLISHED` | Admin | `story.chapter_count + 1`; **không** phát lại thông báo chương mới |

Quy tắc kỹ thuật:
- Bảng chuyển đổi khai báo trong `ChapterStatus.canTransitionTo(target)`; sai ⇒ `ErrorCode.CHAPTER_INVALID_TRANSITION`.
- **Mọi** lần xuất bản/ẩn/hiện đi qua `ChapterPublishService` — nơi duy nhất sửa bộ đếm của truyện và phát sự kiện.
- `ChapterPublishJob` (`fixedDelay` 60 s): lấy id chương `SCHEDULED` có `scheduled_at <= now`, xuất bản **từng chương trong transaction riêng**
  bằng UPDATE có điều kiện `WHERE status = 'SCHEDULED'` ⇒ chạy lặp/chạy trùng không đăng hai lần, một chương lỗi không chặn chương khác. Ghi `job_run`.
- `chapter_no` là số nguyên dương, duy nhất trong truyện; tác giả tự nhập, hệ thống gợi ý `max + 1`. Chương đã `PUBLISHED` vẫn sửa được nội dung, không đổi được số.
- Thời gian hẹn nhập theo giờ Việt Nam ở form, lưu UTC; so sánh bằng `Clock` được inject.

### 4.4. Nội dung chương

**Truyện tranh**
- Ảnh `jpg` / `png` / `webp`; định dạng xác định từ **nội dung tệp** (ImageIO đọc phần đầu ảnh, plugin TwelveMonkeys cho WebP) — đuôi và MIME do trình duyệt gửi không được tin, đuôi lúc lưu suy ra từ định dạng thật; giới hạn dung lượng mỗi ảnh và số ảnh mỗi chương đọc từ `setting`.
- Upload **từng ảnh một request** (JS xếp hàng, có thanh tiến độ) vào chương đang `DRAFT` — không gửi một multipart khổng lồ.
  Thứ tự ban đầu theo **sắp xếp tự nhiên** tên tệp (`2.jpg` trước `10.jpg`); kéo thả để đổi, lưu bằng một request gửi danh sách id theo thứ tự mới.
- Lưu `width`, `height` để trang đọc đặt sẵn tỉ lệ khung (không giật layout); `<img loading="lazy">`.
- Tên lưu = UUID, thư mục `stories/{storyId}/chapters/{chapterId}/`; DB chỉ giữ khóa tương đối, URL do `StorageService.resolveUrl()` dựng.

**Truyện chữ**
- Soạn bằng Quill; lúc lưu **làm sạch bằng jsoup whitelist** (`p, br, strong, em, u, h2, h3, blockquote, hr`), bỏ mọi thuộc tính.
  Đây là **ngoại lệ duy nhất** được dùng `th:utext`, và chỉ với cột đã làm sạch.
- Lưu `word_count` để hiển thị và để chatbot ước lượng độ dài. Nội dung nằm ở bảng riêng `chapter_content` để danh sách chương không kéo `LONGTEXT`.
- Trang đọc có cỡ chữ / nền sáng-tối lưu `localStorage`.

### 4.5. Đọc truyện, lượt xem, xếp hạng

- **Lượt xem**: mỗi lần mở chương phát `ChapterViewedEvent` (xử lý `@Async`); khử trùng lặp theo (phiên, chương) trong `view.dedupe.minutes` bằng cache Caffeine;
  cộng `chapter.view_count`, `story.view_count` và upsert `story_view_daily` (`INSERT ... ON DUPLICATE KEY UPDATE`). Tác giả tự xem truyện mình không tính.
- **Bảng xếp hạng**: theo lượt xem ngày/tuần/tháng (`SUM` trên `story_view_daily`), theo lượt theo dõi, theo điểm đánh giá
  (chỉ tính truyện có ≥ `ranking.min_rating_count` lượt). Kết quả cache 10 phút.
- **Trang chủ**: mới cập nhật (`last_chapter_at`), nổi bật tuần, truyện mới, hoàn thành; người đã đăng nhập có khối "Đọc tiếp".
- **Tìm kiếm & lọc**: từ khóa (FULLTEXT trên tên/tên khác/mô tả, rơi về `LIKE` khi từ khóa quá ngắn) + thể loại (nhiều) + loại + trạng thái + sắp xếp;
  tham số nằm trên query string để chia sẻ link. Logic nằm ở `StoryCatalogQueryService` — **chatbot dùng lại đúng service này**.

### 4.6. Tương tác của độc giả

- **Theo dõi**: bật/tắt, duy nhất (user, story); `follow_count` cập nhật nguyên tử.
- **Lịch sử / đọc tiếp**: một dòng cho mỗi (user, story), upsert chương đang đọc mỗi lần mở chương; trang chi tiết có nút "Đọc tiếp chương N".
- **Đánh giá**: 1–5 sao, mỗi người một lần cho mỗi truyện, sửa được; cập nhật `rating_sum`/`rating_count` theo chênh lệch trong cùng transaction.
- **Bình luận**: text thuần (≤ 1000 ký tự), gắn truyện hoặc chương, trả lời một cấp; giãn cách tối thiểu giữa hai bình luận (`comment.cooldown.seconds`);
  người viết tự xóa, admin ẩn kèm lý do (hiện "Bình luận đã bị ẩn").
- **Báo cáo vi phạm**: đối tượng `STORY` / `CHAPTER` / `COMMENT` + lý do (enum) + mô tả; admin xử lý: bỏ qua hoặc ẩn nội dung (đi qua đúng service kiểm duyệt).

### 4.7. Thông báo

Service phát `ApplicationEvent`; `NotificationListener` dùng `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`.
Chương mới ⇒ ghi cho mọi người theo dõi bằng **một câu `INSERT ... SELECT` từ `story_follow`**, không lặp từng người.
Loại: `NEW_CHAPTER`, `AUTHOR_REQUEST_APPROVED`, `AUTHOR_REQUEST_REJECTED`, `CONTENT_HIDDEN`, `COMMENT_REPLIED`. Chuông polling 60 s.

### 4.8. Chatbot

Tóm tắt (đầy đủ ở [04](04-KE-HOACH-CHATBOT.md)): độc giả nhắn tiếng Việt → Spring AI gửi kèm lịch sử gần nhất và định nghĩa hàm →
mô hình gọi `searchStories` / `getStoryDetail` / `findSimilarStories` / `getTrendingStories` → trả JSON `{reply, recommendations[{storyId, reason}]}` →
server hậu kiểm id, dựng thẻ truyện từ DB → lưu hội thoại. LLM lỗi/hết hạn mức ⇒ rơi về tìm theo từ khóa.

---

## 5. Bảo mật

| Hạng mục | Biện pháp |
|---|---|
| Xác thực | Form login, session cookie `HttpOnly` + `SameSite=Lax` (+ `Secure` ở prod), `sessionFixation().migrateSession()` |
| Mật khẩu | BCrypt; chính sách độ dài/ký tự từ `setting`; khóa tạm sau N lần sai (`failed_attempts`, `locked_until`) |
| Phân quyền | URL matcher theo khu vực + `@PreAuthorize`; **sở hữu dữ liệu kiểm tra ở service** (`StoryAccessPolicy`): tác giả A mở `/studio/chapters/{id của B}` ⇒ 404 |
| Nội dung ẩn | Truy vấn công khai luôn qua `publiclyVisible()`; URL chương nháp/ẩn trả 404 với người không có quyền |
| CSRF | Bật; form Thymeleaf tự chèn; `fetch()` gửi `X-CSRF-TOKEN` |
| XSS | `th:text` mặc định; `th:utext` chỉ cho HTML chương đã qua `HtmlSanitizer`; bình luận/mô tả là text thuần (`white-space: pre-line`) |
| Upload | Chỉ nhận tệp mà ImageIO nhận diện là JPEG/PNG/WebP từ nội dung (không tin đuôi/MIME); giới hạn kích thước/số lượng; tên UUID; chống path traversal; `/media/**` gửi `X-Content-Type-Options: nosniff` |
| Chatbot | Chỉ người đã đăng nhập; giới hạn số tin/ngày và độ dài tin; hàm tra cứu **chỉ đọc**, luôn lọc công khai; `userId` lấy từ `SecurityContext`, không nhận từ mô hình; khóa API qua biến môi trường |
| HTTP header | `X-Frame-Options=DENY`, `Referrer-Policy`, CSP `script-src 'self'` (thêm host Cloudinary vào `img-src` khi bật) |
| Audit | `audit_log` cho: duyệt/từ chối tác giả, ẩn/hiện truyện-chương-bình luận, khóa/đổi vai trò người dùng, đổi setting |

---

## 6. Yêu cầu phi chức năng

- **Hiệu năng**: trang chủ và danh sách < 500 ms với ~5k truyện / 100k chương (index ở [01](01-MO-HINH-DU-LIEU.md)); tránh N+1 bằng `@EntityGraph`/projection;
  cache Caffeine cho thể loại, setting, bảng xếp hạng; ảnh lazy-load, `Cache-Control` dài cho `/media/**` (tên UUID không đổi nội dung).
- **Responsive**: lưới thẻ truyện 2 / 3 / 6 cột theo breakpoint Bootstrap; trang đọc tối ưu một tay trên điện thoại; kiểm thử 360 / 768 / 1440 px.
- **Bảo trì**: tuân thủ [03](03-QUY-TAC-CODE.md); coverage service ≥ 60%.
- **Sẵn sàng**: `docker compose up` + `mvnw spring-boot:run` trên máy sạch là chạy, có dữ liệu demo.

---

## 7. Môi trường & phiên bản

| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| Java | 21 LTS | đề cương yêu cầu 17+; máy phát triển đã có JDK 21 |
| Spring Boot | 3.5.x | cùng bản với dự án mẫu |
| Spring AI | **1.1.8** qua `spring-ai-bom` (dòng 1.1.x dành cho Boot 3.5; dòng 2.x yêu cầu Boot 4) | `spring-ai-starter-model-anthropic`; đổi nhà cung cấp xem [04 §3](04-KE-HOACH-CHATBOT.md) |
| MySQL | 8.4 LTS | `utf8mb4_unicode_ci`; `--innodb-ft-min-token-size=1` để FULLTEXT không bỏ từ tiếng Việt ngắn |
| Flyway | theo BOM + `flyway-mysql` | `ddl-auto: validate` |
| Thymeleaf | 3.1 + Layout Dialect 3.x + `thymeleaf-extras-springsecurity6` | |
| WebJars | bootstrap 5.3, bootstrap-icons, chart.js, quill, sortablejs, webjars-locator-lite | không CDN |
| MapStruct / Lombok | 1.6.x / theo BOM + `lombok-mapstruct-binding` | thứ tự processor như mẫu |
| jsoup | 1.x | làm sạch HTML chương |
| Caffeine, springdoc-openapi | theo BOM / 2.8.x | cache; Swagger chỉ cho `/api/**` |
| Cloudinary SDK | `cloudinary-http5` | chỉ nạp khi `app.storage.provider=CLOUDINARY` |
| Test | JUnit 5, Mockito, Testcontainers MySQL, spring-security-test, JaCoCo | không dùng H2 |

Profile: `application.yml` (chung) · `-dev` (nạp `db/demo`, tắt cache Thymeleaf) · `-prod` · `-test` (Testcontainers).
`docker-compose.yml`: `mysql:8.4` (cổng 3308) + `adminer` (cổng 8082) — image qua mirror `public.ecr.aws` như dự án mẫu vì Docker Hub có thể bị chặn;
cổng lệch khỏi mặc định để chạy song song với dự án khác trên cùng máy, đổi được qua `.env`.

---

## 8. Chiến lược kiểm thử

| Tầng | Phạm vi tối thiểu |
|---|---|
| Unit (JUnit 5 + Mockito) | `ChapterStatus` (mọi cặp chuyển đổi), `StoryAccessPolicy`, `HtmlSanitizer` (bộ payload XSS), `SlugUtils`, `NaturalOrderComparator`, `RatingService` (chênh lệch điểm), `RecommendationValidator` của chatbot |
| Integration (Testcontainers MySQL) | Flyway chạy sạch; đăng ký → đăng nhập → phân quyền URL; gửi → duyệt yêu cầu tác giả → phiên nhận quyền; tạo truyện → đăng chương → người theo dõi nhận thông báo; job hẹn giờ idempotent; truy vấn công khai không lộ nháp/ẩn; hàm chatbot chỉ trả truyện công khai |
| Web/MVC (`@WebMvcTest`) | Khách vào `/studio` ⇒ 302 login; USER vào `/admin` ⇒ 403; POST thiếu CSRF ⇒ 403 |
| Chatbot | LLM giả lập (mock `ChatModel`) cho test tự động; bộ ~30 câu hỏi tiếng Việt chạy với LLM thật để đo (xem [04 §9](04-KE-HOACH-CHATBOT.md)) |
| Thủ công | Checklist demo cuối [02](02-LO-TRINH.md), trên Chrome/Edge + điện thoại thật |

---

## 9. Rủi ro & giảm thiểu

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Chatbot chỉ có 5 ngày trong kế hoạch (19–23/11) | Cao | `StoryCatalogQueryService` làm xong từ GĐ 3 là đã có 80% phần hàm tra cứu; thử "hello tool" với Spring AI ngay GĐ 1 để lộ sớm vấn đề khóa API / mạng |
| Mạng nội bộ chặn API LLM (đã từng chặn Docker Hub) | Cao | Kiểm tra kết nối ở GĐ 1; code chỉ phụ thuộc `ChatClient` nên đổi nhà cung cấp bằng cấu hình; có đường lui tìm theo từ khóa |
| Chatbot bịa truyện / lộ truyện đang ẩn | Cao | Thẻ truyện dựng từ DB theo id đã hậu kiểm; hàm tra cứu dùng chung điều kiện công khai |
| Lộ chương nháp / chương hẹn giờ | Trung bình | Một định nghĩa công khai duy nhất + test tích hợp cho từng URL đọc |
| Bộ đếm lệch (chapter_count, rating, follow) | Trung bình | Chỉ sửa ở một cửa, UPDATE nguyên tử; có test đếm lại so với `COUNT(*)` |
| XSS qua nội dung truyện chữ | Trung bình | Làm sạch lúc lưu, test bộ payload; CSP `script-src 'self'` |
| GĐ đọc truyện (21/10) đến trước GĐ tác giả (29/10) nên chưa có dữ liệu | Trung bình | Viết script sinh dữ liệu demo ngay đầu GĐ 3 |
| Bản quyền nội dung demo | Trung bình | Dùng truyện tự viết/sinh và ảnh giữ chỗ tự tạo; không lấy truyện từ website khác |
| Upload ảnh nặng làm chậm tiến độ | Trung bình | Lưu cục bộ trước, Cloudinary là tùy chọn; không làm resize/thumbnail ở V1 |
| Phình phạm vi (thanh toán, realtime, tìm kiếm ngữ nghĩa) | Trung bình | Đã loại trong đề cương §3; ghi vào "hướng phát triển" |

---

## 10. Tiêu chí hoàn thành (Definition of Done)

1. Có migration Flyway (nếu đụng schema) chạy sạch trên DB trống; entity `validate` không lỗi.
2. Service có Javadoc tiếng Việt, ném `ErrorCode`, không hardcode; mapper MapStruct.
3. Trang Thymeleaf đúng theo cả 4 loại người dùng, responsive, không chuỗi viết cứng, không lỗi console.
4. Nhánh nghiệp vụ chính có test; `mvnw verify` xanh.
5. Thao tác kiểm duyệt/quản trị có `audit_log`.
6. `docs/` cập nhật khi đổi nghiệp vụ hoặc ERD.

## 11. Sản phẩm bàn giao (đề cương §6)

1. Mã nguồn trên GitHub + `docker-compose.yml`, README chạy trong 3 lệnh.
2. Tài liệu phân tích thiết kế: use case, biểu đồ tuần tự (đăng ký tác giả & duyệt; đăng chương hẹn giờ & thông báo; chatbot function calling; đọc chương & đếm lượt xem),
   biểu đồ lớp, ERD — nguồn PlantUML ở `docs/diagrams/`.
3. Dữ liệu demo: ~30 thể loại, ≥ 60 truyện (cả hai loại, đủ trạng thái), ≥ 600 chương, ~20 tài khoản, bình luận/đánh giá/lượt xem rải 60 ngày để bảng xếp hạng và biểu đồ có hình.
4. Tài khoản demo cho 3 vai trò; hướng dẫn cài đặt; báo cáo đồ án + slide.
