# Quy tắc viết code Back-end & Thymeleaf

> Quy chuẩn bắt buộc cho toàn bộ `backend/`. Kế thừa từ `it-support-management/docs/03-QUY-TAC-CODE-BE.md` — tài liệu đó có ví dụ đầy đủ hơn cho từng mục;
> ở đây đổi ví dụ sang nghiệp vụ truyện và ghi những điểm **khác** (Bootstrap, nội dung công khai, HTML chương, chatbot). Bản rút gọn ở [`../CLAUDE.md`](../CLAUDE.md).
>
> Nguyên tắc bao trùm: **tên tiếng Anh — comment tiếng Việt — không hardcode — không tự viết hàm map — không chuỗi hiển thị trong Java lẫn template.**

## 1. Không hardcode

Trong `service`, `controller`, `repository`, `job`, `tool` không được có số hoặc chuỗi mang ý nghĩa nghiệp vụ.

| Loại giá trị | Ví dụ | Nơi khai báo |
|---|---|---|
| Quy tắc nghiệp vụ **cố định** | `MAX_TITLE_LENGTH = 200`, `RATING_MIN = 1`, `SLUG_MAX_LENGTH = 220` | `common/constant/*Constants.java` |
| Tham số **đổi được khi vận hành** | dung lượng ảnh tối đa, giãn cách bình luận, hạn mức chat/ngày | bảng `setting` qua `SettingService` |
| Cấu hình theo môi trường | thư mục lưu ảnh, nhà cung cấp lưu trữ, tên mô hình LLM, timeout, chu kỳ job | `application.yml` → `@ConfigurationProperties` |
| Tập giá trị đóng | `StoryType`, `StoryVisibility`, `ChapterStatus`, `ReportReason` | `enum` trong package tính năng |
| Chuỗi hiển thị | nhãn nút, thông báo lỗi, tiêu đề cột | `i18n/messages.properties` |
| Tên view, redirect | `"studio/story-form"`, `"redirect:/studio/stories"` | `ViewConstants` |
| Văn bản cho mô hình đọc | system prompt, mô tả hàm | `resources/prompts/*.txt`, `ChatToolDescriptions` |

```java
// ❌ SAI: số 15 và chuỗi lỗi viết cứng, thời gian lấy trực tiếp
if (lastComment.getCreatedAt().plusSeconds(15).isAfter(Instant.now())) {
    throw new RuntimeException("Bạn bình luận quá nhanh");
}

// ✅ ĐÚNG: tham số từ setting, lỗi theo mã, thời gian từ Clock được inject
int cooldownSeconds = settingService.getInt(SettingKeys.COMMENT_COOLDOWN_SECONDS, CommentConstants.DEFAULT_COOLDOWN_SECONDS);
if (lastComment.getCreatedAt().plusSeconds(cooldownSeconds).isAfter(clock.instant())) {
    throw new ApiException(ErrorCode.COMMENT_TOO_FAST, cooldownSeconds);
}
```

Hằng số: `public final class` + constructor `private`; tên `UPPER_SNAKE_CASE` mang nghĩa nghiệp vụ; không dùng `interface` chứa hằng.
Cấu hình có kiểu bằng `record` + `@ConfigurationProperties` + `@Validated`, không rải `@Value`.

### Enum mang hành vi

```java
/** Trạng thái chương. Bảng chuyển đổi hợp lệ nằm ở đây, service chỉ hỏi (docs/00 §4.3). */
public enum ChapterStatus {
    DRAFT, SCHEDULED, PUBLISHED, HIDDEN;

    private static final Map<ChapterStatus, Set<ChapterStatus>> TRANSITIONS = Map.of(
            DRAFT,     EnumSet.of(SCHEDULED, PUBLISHED),
            SCHEDULED, EnumSet.of(DRAFT, PUBLISHED),
            PUBLISHED, EnumSet.of(HIDDEN),
            HIDDEN,    EnumSet.of(PUBLISHED));

    public boolean canTransitionTo(ChapterStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    /** Chỉ chương đã đăng mới tính vào số chương của truyện và hiện với độc giả. */
    public boolean isPubliclyReadable() {
        return this == PUBLISHED;
    }
}
```

## 2. Thông báo hiển thị

Không một chuỗi hiển thị nào nằm trong Java hay template. `messages.properties` là tiếng Việt mặc định; cơ chế i18n giữ nguyên như mẫu.

```properties
error.story.not.found=Không tìm thấy truyện
error.chapter.invalid.transition=Không thể chuyển chương từ trạng thái {0} sang {1}
error.chapter.schedule.in.past=Thời điểm hẹn đăng phải ở tương lai
error.author.request.already.pending=Bạn đang có một yêu cầu chờ duyệt
error.chat.daily.limit.exceeded=Bạn đã dùng hết {0} lượt hỏi hôm nay
validation.story.title.required=Vui lòng nhập tên truyện
enum.StoryType.COMIC=Truyện tranh
enum.StoryType.NOVEL=Truyện chữ
enum.ChapterStatus.SCHEDULED=Hẹn giờ
notification.NEW_CHAPTER={0} vừa ra chương {1}
flash.chapter.published=Đã đăng chương {0}
```

- Khóa khai báo trong `MessageKeys`; service ném `ApiException(ErrorCode.X, args…)`; `ErrorCode` gắn `HttpStatus` + khóa.
- `GlobalExceptionHandler` là nơi **duy nhất** dịch: request `/api/**` hoặc `Accept: application/json` → `ApiResponse.error`, còn lại → trang lỗi.
- Lỗi nghiệp vụ trong form POST: controller bắt `ApiException`, đưa message vào flash, redirect về trang đang thao tác.
- **Nội dung công khai không tồn tại hoặc không được phép xem ⇒ luôn 404** (`STORY_NOT_FOUND`, `CHAPTER_NOT_FOUND`), không trả 403 — tránh lộ sự tồn tại của bản nháp.
- Validation dùng khóa: `@NotBlank(message = "{validation.story.title.required}")`.

## 3. Đặt tên (tiếng Anh)

Toàn bộ định danh — package, class, method, biến, bảng, cột, khóa messages, id/class HTML, tên template, URL — bằng tiếng Anh.

| Loại | Hậu tố | Ví dụ |
|---|---|---|
| MVC controller (trả view) | `Controller` | `StudioChapterController`, `AdminStoryController` |
| REST controller (JSON) | `ApiController` | `ChatApiController`, `InteractionApiController` |
| Nghiệp vụ | `Service` / `ServiceImpl` | `ChapterPublishService` |
| Chỉ đọc phục vụ nhiều nơi | `QueryService` | `StoryCatalogQueryService` |
| Chính sách quyền | `Policy` | `StoryAccessPolicy` |
| Truy cập dữ liệu | `Repository` / `Specification` | `StoryRepository`, `StorySpecification` |
| Ánh xạ | `Mapper` | `StoryMapper` |
| Form binding (có setter) | `Form` | `StoryForm`, `ChapterScheduleForm` |
| Dữ liệu vào / ra (`record`) | `Request` / `Response` | `StoryFilterRequest`, `StoryCardResponse` |
| Thực thể JPA | *(không hậu tố)* | `Story`, `ChapterPage` |
| Sự kiện / lắng nghe | `Event` / `Listener` | `ChapterPublishedEvent`, `NotificationListener` |
| Tác vụ định kỳ | `Job` | `ChapterPublishJob` |
| Hàm cho LLM | `Tools` | `StoryCatalogTools` |

Tiền tố method: `get…` (chắc chắn có, không có thì ném lỗi) · `find…` (`Optional`) · `search…` (phân trang) · `create/update/delete…` ·
`change…` / `publish` / `hide` / `restore` (đổi trạng thái có tác dụng phụ) · `toggle…` · `is/has/can…` · `validate…` · `build…` · `calculate…` · `handle…` (sự kiện) · `record…` (ghi vết).
Cấm `doSomething`, `process`, `handleData`, `tmp`, `list1`.

Thời gian: hậu tố `At` cho mốc (`publishedAt`, `scheduledAt`), `Count` cho bộ đếm, collection số nhiều.

## 4. Comment tiếng Việt

Giải thích **tại sao**, không lặp lại code. Javadoc bắt buộc cho method `public` của service/controller và mọi lớp `public`.

```java
// Xuất bản bằng UPDATE có điều kiện thay vì đọc-rồi-ghi: job chạy trùng hoặc tác giả bấm "Đăng ngay"
// đúng lúc job quét thì chỉ một bên cập nhật được, bên kia nhận 0 dòng và bỏ qua —
// nhờ vậy chapter_count không bị cộng hai lần và người theo dõi không nhận hai thông báo.
int updatedRows = chapterRepository.publishIfScheduled(chapterId, now);
if (updatedRows == 0) {
    return;
}
```

Không dùng comment để giữ code cũ.

## 5. Controller

| | `@Controller` (Thymeleaf) | `@RestController` (`/api/**`) |
|---|---|---|
| Trả về | tên view hoặc `redirect:` | `ApiResponse<T>` |
| Đầu vào | `@ModelAttribute @Valid *Form` + `BindingResult` | `@RequestBody @Valid` hoặc multipart |
| Lỗi validation | render lại form với `th:errors` | JSON `errors[]` |
| Sau POST thành công | **PRG**: flash + `redirect:` | 200/201 JSON |
| Swagger | không | `@Tag` + `@Operation`; DTO có `@Schema` |

Controller: nhận DTO → gọi service → đưa DTO vào `Model` → trả view. **Cấm** `if` nghiệp vụ, gọi repository, đưa Entity vào `Model`.
Quyền theo vai trò ở `SecurityConfig` + `@PreAuthorize`; quyền theo **dữ liệu** (truyện này của ai) ở `StoryAccessPolicy` trong service.

## 6. MapStruct

Cấm hàm map viết tay. `componentModel = spring`, `unmappedTargetPolicy = ERROR` (đặt ở compiler args như mẫu). Thứ tự processor: lombok → lombok-mapstruct-binding → mapstruct.
Trường do service quyết định (slug, tác giả, bộ đếm, trạng thái) phải `@Mapping(target = …, ignore = true)` tường minh khi map từ form.
Trường tính toán (`ratingAverage`, URL ảnh qua `StorageService.resolveUrl`) đặt trong `default`/`@Named` method của mapper hoặc để service điền. Mapper không gọi repository.

## 7. Chia hàm & trách nhiệm tầng

| Tiêu chí | Giới hạn |
|---|---|
| Số dòng một method | ≤ 30 |
| Cognitive Complexity | ≤ 15 |
| Độ sâu lồng nhau | ≤ 3 (return sớm) |
| Số tham số | ≤ 5, nhiều hơn thì gom `record` |
| Số dòng một lớp | ≤ 500 |

Controller (DTO ↔ service) · Service (nghiệp vụ, `@Transactional`) · Repository (truy vấn) · Mapper (ánh xạ) · Job (gọi service, ghi `job_run`, không chứa nghiệp vụ) · Tools (gọi `QueryService`, không chứa nghiệp vụ).

**Các cửa duy nhất — không đi đường tắt:**

| Việc | Chỉ được làm ở |
|---|---|
| Xuất bản / ẩn / hiện chương, sửa `story.chapter_count`, `last_chapter_at` | `ChapterPublishService` |
| Truy vấn truyện/chương cho người đọc và cho chatbot | `StoryCatalogQueryService`, `ChapterReaderService` (luôn áp `publiclyVisible`) |
| Ẩn / hiện truyện, bình luận | `StoryModerationService`, `CommentService` (ghi audit + thông báo) |
| Đổi vai trò / trạng thái tài khoản | `AuthorRequestService.approve`, `AdminUserService` (evict cache quyền) |
| Lưu / xóa / dựng URL ảnh | `StorageService` |
| Làm sạch HTML chương | `HtmlSanitizer`, gọi lúc **lưu** |

## 8. Sạch với SonarQube

- Không `System.out`; log dùng placeholder `log.info("Đã đăng chương {} của truyện {}", chapterId, storyId)`; `log.error(…, exception)` giữ stacktrace.
- Không log mật khẩu, khóa API, nội dung tin nhắn chat ở mức INFO.
- Không `RuntimeException` trần, không nuốt ngoại lệ, không field injection (`@RequiredArgsConstructor` + `final`), không trả `null` cho collection, DTO là `record`.
- Job bắt ngoại lệ ở mức từng phần tử để một chương lỗi không dừng cả lượt quét; đếm lỗi vào `job_run`.
- Native query dùng named parameter, không nối chuỗi.

## 9. Thymeleaf & static

1. Layout Dialect: `layout:decorate="~{layout/site}"` / `~{layout/studio}` / `~{layout/admin}`; nội dung ở `layout:fragment="content"`, script riêng ở `layout:fragment="scripts"`.
2. Không chuỗi viết cứng: `th:text="#{story.list.title}"`; enum: `th:text="#{'enum.StoryType.' + ${story.type}}"`.
3. Chỉ DTO trong Model; `open-in-view: false`.
4. Form: `th:object="${form}"` + `th:field` + `th:errors` gắn lớp `*Form` (có setter, Bean Validation); `th:action` để Thymeleaf tự chèn CSRF.
5. `sec:authorize` chỉ để **ẩn menu/nút**; dữ liệu không được phép xem phải được service loại trước khi vào Model.
6. **`th:utext` bị cấm, trừ đúng một chỗ**: nội dung chương truyện chữ ở `chapter/read-novel.html`, đọc từ cột đã qua `HtmlSanitizer`. Mô tả truyện, bình luận, giới thiệu tác giả là text thuần (`th:text` + CSS `white-space: pre-line`).
7. Fragment tham số hóa: `th:replace="~{fragments/story-card :: card(${story})}"`. Không lặp markup thẻ truyện/badge/phân trang.
8. **Bootstrap 5**: ưu tiên class tiện ích và component có sẵn; CSS riêng chỉ ở `static/css/app.css` (biến màu chủ đề, trang đọc, khung chat). Không `style=""` inline.
9. Thư viện qua WebJars (`/webjars/bootstrap/...`), không CDN.
10. JS ở `static/js/*.js`, không inline `onclick`; `fetch()` qua `App.fetchJson()`; dữ liệu trả về chèn bằng `textContent`, không `innerHTML` với dữ liệu người dùng.
11. Ảnh truyện: `<img loading="lazy" width height>`; URL lấy từ DTO (đã qua `StorageService.resolveUrl`), template không tự ghép đường dẫn.
12. Thời gian: DTO trả `Instant`; hiển thị `#temporals.format(…, zone)` với `zone` từ `GlobalModelAttributes`.
13. Lọc/phân trang giữ tham số trên query string.

## 10. MySQL & Flyway

1. Schema chỉ qua `db/migration/V{n}__{mo_ta}.sql`; không sửa migration đã chạy; `ddl-auto: validate`.
2. `DATETIME(6)` + `Instant` + `hibernate.jdbc.time_zone: UTC`.
3. Enum `VARCHAR` + `CHECK`; thêm giá trị enum ⇒ migration sửa CHECK.
4. Bộ đếm cập nhật bằng `@Modifying` `UPDATE … SET x = x + :delta`; không `entity.setCount(entity.getCount() + 1)`.
5. Thao tác tranh chấp dùng UPDATE có điều kiện (xuất bản chương hẹn giờ); form sửa truyện/chương dùng `@Version`.
6. Upsert bằng `INSERT … ON DUPLICATE KEY UPDATE` (lượt xem theo ngày, tiến độ đọc, đánh giá).
7. Danh sách: không kéo `chapter_content`; dùng projection/`@EntityGraph`, không để N+1 ở lưới thẻ truyện.
8. Test dùng Testcontainers `mysql:8.4` với migration thật; không H2 (khác CHECK, FULLTEXT, cột sinh, `ON DUPLICATE KEY`).
9. Dữ liệu demo ở `db/demo/V100+`, chỉ nạp ở profile `dev`.

## 11. Checklist trước khi commit

- [ ] Không còn số/chuỗi nghiệp vụ viết cứng trong Java và template.
- [ ] Chuỗi hiển thị mới có khóa trong `messages.properties`; enum mới có đủ `enum.<Enum>.<VALUE>`.
- [ ] Truy vấn mới phía người đọc có áp `publiclyVisible`; có test "không lộ nháp/ẩn".
- [ ] Thay đổi trạng thái chương/truyện đi đúng cửa ở mục 7; bộ đếm cập nhật nguyên tử.
- [ ] Endpoint `/studio/**`, `/api/studio/**` mới có kiểm tra sở hữu qua `StoryAccessPolicy`.
- [ ] Thao tác quản trị có `audit_log`.
- [ ] Không thêm `th:utext`; JS không dùng `innerHTML` với dữ liệu người dùng.
- [ ] Mapper MapStruct không báo unmapped; Javadoc tiếng Việt cho method public mới.
- [ ] `mvnw verify` xanh.
