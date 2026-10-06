# Mô hình dữ liệu — MySQL 8.4

> Đi kèm [00-KE-HOACH-TONG-THE.md](00-KE-HOACH-TONG-THE.md). Schema tạo bằng **Flyway** (`V1__init_schema.sql`, `V2__seed_master_data.sql`, `V3__chatbot.sql`),
> Hibernate chỉ `ddl-auto: validate`. Dữ liệu demo ở `db/demo/V100+` (chỉ profile `dev`).
>
> DDL dưới đây **lược bốn cột audit** (`created_at`, `updated_at`, `created_by`, `updated_by`) cho gọn — migration thật phải có đủ ở các bảng kế thừa `AuditableEntity`.

## 0. Quy ước chung

| Quy ước | Nội dung |
|---|---|
| Đặt tên | `snake_case`, bảng số ít (`story`, `user_account`), bảng nối `a_b` |
| Khóa chính | `id BIGINT AUTO_INCREMENT PRIMARY KEY` |
| Charset | `utf8mb4` / `utf8mb4_unicode_ci` cho toàn DB (không phân biệt hoa thường và dấu thanh/dấu mũ ⇒ gõ không dấu vẫn tìm được; riêng "đ" KHÔNG bằng "d" — đã kiểm chứng ở GĐ 3: `'tu tiên' = 'TU TIEN'` đúng, `'đô thị' = 'do thi'` sai) |
| Thời gian | `DATETIME(6)` lưu **UTC** (`hibernate.jdbc.time_zone=UTC`), entity `Instant`; hiển thị `Asia/Ho_Chi_Minh` ở view |
| Enum | `VARCHAR` + `CHECK (... IN (...))`; entity `@Enumerated(STRING)`. Không dùng kiểu `ENUM` của MySQL |
| Boolean | `TINYINT(1)`, tên cột `is_*` |
| Xóa | Truyện xóa mềm (`deleted_at`); chương/bình luận dùng trạng thái; `story_follow`, `notification`, `chapter_page` xóa cứng |
| Tệp | Cột `*_path` lưu **khóa tương đối** (ví dụ `stories/12/cover/uuid.jpg`), không lưu URL đầy đủ — đổi nơi lưu không phải sửa dữ liệu |
| Bộ đếm | Cột `*_count` phi chuẩn hóa, chỉ cập nhật bằng `SET x = x + :delta` |
| Đặt tên ràng buộc | `fk_<bang>_<cot>`, `idx_<bang>_<cot...>`, `uk_<bang>_<cot>`, `ck_<bang>_<cot>` |

---

## 1. Sơ đồ quan hệ (ERD dạng văn bản)

```
 user_account ──1:0..1──▶ author_profile
      │  └──1:N──▶ author_request (reviewed_by → user_account)
      │
      │ author_id
      ▼
    story ◀──N:M (story_genre)──▶ genre
      │
      ├──1:N──▶ chapter ──1:0..1──▶ chapter_content      (truyện chữ)
      │            └────1:N─────▶ chapter_page           (truyện tranh)
      │
      ├──1:N──▶ story_follow      (user_id)        duy nhất (user, story)
      ├──1:N──▶ reading_history   (user_id, chapter_id)   duy nhất (user, story)
      ├──1:N──▶ story_rating      (user_id)        duy nhất (user, story)
      ├──1:N──▶ comment           (user_id, chapter_id?, parent_id?)
      └──1:N──▶ story_view_daily  (view_date)

 report (reporter_id, target_type + target_id) · notification (recipient_id)
 chat_conversation (user_id) ──1:N──▶ chat_message
 setting · audit_log · job_run
```

Nguồn PlantUML để xuất ảnh ERD cho báo cáo: `docs/diagrams/erd.puml` (tạo ở GĐ 0).

---

## 2. Người dùng & tác giả

```sql
CREATE TABLE user_account (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  username        VARCHAR(50)  NOT NULL,
  email           VARCHAR(255) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  display_name    VARCHAR(100) NOT NULL,
  avatar_path     VARCHAR(500) NULL,
  bio             VARCHAR(500) NULL,
  role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
  status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  failed_attempts INT          NOT NULL DEFAULT 0,
  locked_until    DATETIME(6)  NULL,
  last_login_at   DATETIME(6)  NULL,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_email (email),
  CONSTRAINT ck_user_role   CHECK (role IN ('USER', 'AUTHOR', 'ADMIN')),
  CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE', 'BANNED'))
);
```

Mỗi người **đúng một** vai trò; "cấp thêm quyền AUTHOR" = đổi `USER → AUTHOR`, quyền độc giả giữ nguyên nhờ `RoleHierarchy` (`AUTHOR > USER`).

```sql
CREATE TABLE author_profile (
  user_id     BIGINT PRIMARY KEY,                  -- 1:1 với user_account, tạo lúc yêu cầu được duyệt
  pen_name    VARCHAR(100) NOT NULL,
  bio         VARCHAR(1000) NULL,
  approved_at DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_author_profile_pen_name (pen_name),
  CONSTRAINT fk_author_profile_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE TABLE author_request (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT        NOT NULL,
  pen_name        VARCHAR(100)  NOT NULL,
  introduction    VARCHAR(2000) NOT NULL,
  intended_type   VARCHAR(20)   NOT NULL,           -- loại truyện dự định sáng tác
  status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  reject_reason   VARCHAR(1000) NULL,
  reviewed_by     BIGINT        NULL,
  reviewed_at     DATETIME(6)   NULL,
  -- MySQL không có unique index có điều kiện: cột sinh này chỉ khác NULL khi đang chờ duyệt,
  -- nên UNIQUE trên nó đảm bảo mỗi người tối đa một yêu cầu PENDING ngay ở tầng DB.
  pending_user_id BIGINT GENERATED ALWAYS AS (IF(status = 'PENDING', user_id, NULL)) STORED,
  UNIQUE KEY uk_author_request_pending (pending_user_id),
  INDEX idx_author_request_status (status, created_at),
  CONSTRAINT fk_author_request_user     FOREIGN KEY (user_id)     REFERENCES user_account (id),
  CONSTRAINT fk_author_request_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account (id),
  CONSTRAINT ck_author_request_type   CHECK (intended_type IN ('COMIC', 'NOVEL', 'BOTH')),
  CONSTRAINT ck_author_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
```

`pending_user_id` không map vào entity (Hibernate `validate` chỉ kiểm tra cột được map).

---

## 3. Thể loại & truyện

```sql
CREATE TABLE genre (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,                -- 'Tu tiên'
  slug        VARCHAR(120) NOT NULL,                -- 'tu-tien' — chatbot dùng slug làm tham số hàm
  description VARCHAR(500) NULL,                    -- đưa vào prompt để mô hình hiểu thể loại
  sort_order  INT          NOT NULL DEFAULT 0,
  is_active   TINYINT(1)   NOT NULL DEFAULT 1,
  UNIQUE KEY uk_genre_name (name),
  UNIQUE KEY uk_genre_slug (slug)
);

CREATE TABLE story (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  slug            VARCHAR(220) NOT NULL,
  title           VARCHAR(200) NOT NULL,
  alt_title       VARCHAR(200) NULL,                -- tên khác, phục vụ tìm kiếm
  description     TEXT         NOT NULL,            -- text thuần
  cover_path      VARCHAR(500) NULL,
  type            VARCHAR(10)  NOT NULL,            -- khóa sau khi có chương đầu tiên
  status          VARCHAR(20)  NOT NULL DEFAULT 'ONGOING',
  visibility      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
  hidden_reason   VARCHAR(1000) NULL,
  author_id       BIGINT       NOT NULL,
  chapter_count   INT          NOT NULL DEFAULT 0,  -- chỉ đếm chương PUBLISHED
  view_count      BIGINT       NOT NULL DEFAULT 0,
  follow_count    INT          NOT NULL DEFAULT 0,
  rating_sum      INT          NOT NULL DEFAULT 0,
  rating_count    INT          NOT NULL DEFAULT 0,
  comment_count   INT          NOT NULL DEFAULT 0,
  published_at    DATETIME(6)  NULL,                -- lần đầu công khai
  last_chapter_at DATETIME(6)  NULL,                -- sắp xếp "mới cập nhật"
  deleted_at      DATETIME(6)  NULL,
  version         INT          NOT NULL DEFAULT 0,
  UNIQUE KEY uk_story_slug (slug),
  INDEX idx_story_author (author_id),
  INDEX idx_story_public_updated (visibility, deleted_at, last_chapter_at),
  INDEX idx_story_public_type_status (visibility, type, status),
  FULLTEXT KEY ft_story_search (title, alt_title, description),
  CONSTRAINT fk_story_author FOREIGN KEY (author_id) REFERENCES user_account (id),
  CONSTRAINT ck_story_type       CHECK (type IN ('COMIC', 'NOVEL')),
  CONSTRAINT ck_story_status     CHECK (status IN ('ONGOING', 'COMPLETED', 'PAUSED')),
  CONSTRAINT ck_story_visibility CHECK (visibility IN ('DRAFT', 'PUBLISHED', 'HIDDEN'))
);

CREATE TABLE story_genre (
  story_id BIGINT NOT NULL,
  genre_id BIGINT NOT NULL,
  PRIMARY KEY (story_id, genre_id),
  INDEX idx_story_genre_genre (genre_id, story_id),
  CONSTRAINT fk_story_genre_story FOREIGN KEY (story_id) REFERENCES story (id),
  CONSTRAINT fk_story_genre_genre FOREIGN KEY (genre_id) REFERENCES genre (id)
);
```

Điểm trung bình = `rating_sum / rating_count`, tính ở mapper; không lưu cột số thực để khỏi lệch do làm tròn.
`version` (`@Version`) chỉ bảo vệ form sửa thông tin truyện; các bộ đếm cập nhật bằng câu UPDATE riêng nên **không** tăng `version`.

---

## 4. Chương

```sql
CREATE TABLE chapter (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  story_id      BIGINT       NOT NULL,
  chapter_no    INT          NOT NULL,
  title         VARCHAR(200) NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
  scheduled_at  DATETIME(6)  NULL,
  published_at  DATETIME(6)  NULL,
  hidden_reason VARCHAR(1000) NULL,
  page_count    INT          NOT NULL DEFAULT 0,    -- truyện tranh
  word_count    INT          NOT NULL DEFAULT 0,    -- truyện chữ
  view_count    BIGINT       NOT NULL DEFAULT 0,
  version       INT          NOT NULL DEFAULT 0,
  UNIQUE KEY uk_chapter_story_no (story_id, chapter_no),
  INDEX idx_chapter_story_status (story_id, status, chapter_no),
  INDEX idx_chapter_scheduled (status, scheduled_at),      -- cho ChapterPublishJob
  CONSTRAINT fk_chapter_story FOREIGN KEY (story_id) REFERENCES story (id),
  CONSTRAINT ck_chapter_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'HIDDEN')),
  CONSTRAINT ck_chapter_no CHECK (chapter_no > 0)
);

-- Tách khỏi chapter để danh sách chương và trang quản lý không kéo LONGTEXT
CREATE TABLE chapter_content (
  chapter_id BIGINT PRIMARY KEY,
  content    LONGTEXT NOT NULL,                     -- HTML ĐÃ làm sạch bằng HtmlSanitizer
  CONSTRAINT fk_chapter_content_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id)
);

CREATE TABLE chapter_page (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  chapter_id BIGINT       NOT NULL,
  page_no    INT          NOT NULL,
  image_path VARCHAR(500) NOT NULL,
  width      INT          NOT NULL,
  height     INT          NOT NULL,
  size_bytes BIGINT       NOT NULL,
  INDEX idx_chapter_page_order (chapter_id, page_no),
  CONSTRAINT fk_chapter_page_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id)
);
```

`chapter_page` **không** đặt UNIQUE `(chapter_id, page_no)`: đổi thứ tự là cập nhật hàng loạt `page_no`, ràng buộc duy nhất sẽ vấp giá trị trung gian. Thứ tự đúng do service đảm bảo (ghi lại 1..n theo danh sách id gửi lên).

---

## 5. Tương tác của độc giả

```sql
CREATE TABLE story_follow (
  user_id    BIGINT NOT NULL,
  story_id   BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, story_id),
  INDEX idx_story_follow_story (story_id),            -- phát thông báo chương mới
  CONSTRAINT fk_story_follow_user  FOREIGN KEY (user_id)  REFERENCES user_account (id),
  CONSTRAINT fk_story_follow_story FOREIGN KEY (story_id) REFERENCES story (id)
);

CREATE TABLE reading_history (
  user_id    BIGINT NOT NULL,
  story_id   BIGINT NOT NULL,
  chapter_id BIGINT NOT NULL,                         -- chương đang đọc dở
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id, story_id),
  INDEX idx_reading_history_recent (user_id, updated_at),
  CONSTRAINT fk_reading_history_user    FOREIGN KEY (user_id)    REFERENCES user_account (id),
  CONSTRAINT fk_reading_history_story   FOREIGN KEY (story_id)   REFERENCES story (id),
  CONSTRAINT fk_reading_history_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id)
);

CREATE TABLE story_rating (
  user_id    BIGINT  NOT NULL,
  story_id   BIGINT  NOT NULL,
  stars      TINYINT NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id, story_id),
  CONSTRAINT fk_story_rating_user  FOREIGN KEY (user_id)  REFERENCES user_account (id),
  CONSTRAINT fk_story_rating_story FOREIGN KEY (story_id) REFERENCES story (id),
  CONSTRAINT ck_story_rating_stars CHECK (stars BETWEEN 1 AND 5)
);

CREATE TABLE comment (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  story_id      BIGINT        NOT NULL,
  chapter_id    BIGINT        NULL,                   -- NULL = bình luận ở trang truyện
  user_id       BIGINT        NOT NULL,
  parent_id     BIGINT        NULL,                   -- trả lời một cấp
  content       VARCHAR(1000) NOT NULL,               -- text thuần
  status        VARCHAR(20)   NOT NULL DEFAULT 'VISIBLE',
  hidden_reason VARCHAR(500)  NULL,
  created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_comment_story (story_id, chapter_id, created_at),
  INDEX idx_comment_user (user_id, created_at),
  CONSTRAINT fk_comment_story   FOREIGN KEY (story_id)   REFERENCES story (id),
  CONSTRAINT fk_comment_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id),
  CONSTRAINT fk_comment_user    FOREIGN KEY (user_id)    REFERENCES user_account (id),
  CONSTRAINT fk_comment_parent  FOREIGN KEY (parent_id)  REFERENCES comment (id),
  CONSTRAINT ck_comment_status CHECK (status IN ('VISIBLE', 'HIDDEN', 'DELETED'))
);

CREATE TABLE report (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  reporter_id     BIGINT        NOT NULL,
  target_type     VARCHAR(20)   NOT NULL,
  target_id       BIGINT        NOT NULL,             -- đa hình, không FK; service kiểm tra tồn tại
  reason          VARCHAR(30)   NOT NULL,
  detail          VARCHAR(1000) NULL,
  status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  handled_by      BIGINT        NULL,
  handled_at      DATETIME(6)   NULL,
  resolution_note VARCHAR(1000) NULL,
  created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_report_status (status, created_at),
  INDEX idx_report_target (target_type, target_id),
  CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES user_account (id),
  CONSTRAINT fk_report_handler  FOREIGN KEY (handled_by)  REFERENCES user_account (id),
  CONSTRAINT ck_report_target CHECK (target_type IN ('STORY', 'CHAPTER', 'COMMENT')),
  CONSTRAINT ck_report_reason CHECK (reason IN ('COPYRIGHT', 'ADULT_CONTENT', 'VIOLENCE', 'SPAM', 'HARASSMENT', 'OTHER')),
  CONSTRAINT ck_report_status CHECK (status IN ('PENDING', 'RESOLVED', 'DISMISSED'))
);
```

---

## 6. Thông báo & thống kê

```sql
CREATE TABLE notification (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_id BIGINT       NOT NULL,
  type         VARCHAR(40)  NOT NULL,
  message_args JSON         NULL,                    -- tham số cho khóa messages 'notification.<TYPE>' (tên truyện, số chương, lý do)
  link         VARCHAR(500) NULL,                    -- đường dẫn tương đối
  is_read      TINYINT(1)   NOT NULL DEFAULT 0,
  created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_notification_recipient (recipient_id, is_read, created_at),
  CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES user_account (id),
  CONSTRAINT ck_notification_type CHECK (type IN
    ('NEW_CHAPTER', 'AUTHOR_REQUEST_APPROVED', 'AUTHOR_REQUEST_REJECTED', 'CONTENT_HIDDEN', 'COMMENT_REPLIED'))
);

-- Lượt xem theo ngày: nguồn cho bảng xếp hạng ngày/tuần/tháng và biểu đồ của tác giả
CREATE TABLE story_view_daily (
  story_id   BIGINT NOT NULL,
  view_date  DATE   NOT NULL,                        -- ngày theo giờ Việt Nam
  view_count INT    NOT NULL DEFAULT 0,
  PRIMARY KEY (story_id, view_date),
  INDEX idx_story_view_daily_date (view_date, view_count),
  CONSTRAINT fk_story_view_daily_story FOREIGN KEY (story_id) REFERENCES story (id)
);
```

Thông báo lưu **loại + tham số**, không lưu câu chữ: câu hiển thị dựng từ `messages.properties` lúc render nên sửa lời văn không phải sửa dữ liệu.

---

## 7. Chatbot (`V3__chatbot.sql`)

```sql
CREATE TABLE chat_conversation (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NOT NULL,
  title           VARCHAR(200) NULL,                 -- cắt từ tin nhắn đầu tiên
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_message_at DATETIME(6)  NOT NULL,
  INDEX idx_chat_conversation_user (user_id, last_message_at),
  CONSTRAINT fk_chat_conversation_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE TABLE chat_message (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id   BIGINT      NOT NULL,
  role              VARCHAR(20) NOT NULL,
  content           TEXT        NOT NULL,            -- lời người dùng / lời đáp của trợ lý
  recommendations   JSON        NULL,                -- [{storyId, reason}] ĐÃ hậu kiểm
  tool_trace        JSON        NULL,                -- [{tool, arguments, storyIds}] để hỏi nối tiếp và để chẩn đoán
  answer_source     VARCHAR(20) NULL,                -- LLM / FALLBACK_KEYWORD
  fallback_reason   VARCHAR(50) NULL,
  prompt_tokens     INT         NULL,
  completion_tokens INT         NULL,
  latency_ms        INT         NULL,
  feedback          TINYINT     NULL,                -- 1 = hữu ích, -1 = không
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_chat_message_conversation (conversation_id, id),
  INDEX idx_chat_message_created (created_at),
  CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation (id),
  CONSTRAINT ck_chat_message_role CHECK (role IN ('USER', 'ASSISTANT'))
);
```

---

## 8. Hệ thống

Sao chép từ dự án mẫu (đổi tên cột nếu cần): `setting` (`setting_key`, `setting_value`, `value_type`, `description`, `group_name`),
`audit_log` (`actor_id`, `action`, `entity_type`, `entity_id`, `detail` JSON, `ip_address`, `created_at`), `job_run` (`job_name`, `started_at`, `finished_at`, `processed_count`, `error_count`, `error_message`).

### Khóa `setting` khởi tạo

| Khóa | Mặc định | Ý nghĩa |
|---|---|---|
| `security.password.min_length` | 8 | Độ dài mật khẩu tối thiểu |
| `security.login.max_failed_attempts` / `security.login.lock_minutes` | 5 / 15 | Khóa tạm khi sai mật khẩu |
| `author.request.cooldown.days` | 7 | Số ngày chờ trước khi gửi lại yêu cầu bị từ chối |
| `upload.image.max_size_mb` | 5 | Dung lượng tối đa mỗi ảnh |
| `upload.chapter.max_pages` | 150 | Số ảnh tối đa mỗi chương |
| `comment.cooldown.seconds` | 15 | Giãn cách giữa hai bình luận |
| `view.dedupe.minutes` | 30 | Cửa sổ khử trùng lặp lượt xem |
| `ranking.min_rating_count` | 5 | Số lượt đánh giá tối thiểu để vào bảng xếp hạng điểm |
| `chat.enabled` | true | Bật/tắt chatbot |
| `chat.daily_limit_per_user` | 50 | Số tin nhắn mỗi người mỗi ngày |
| `chat.max_message_length` | 500 | Độ dài tối đa một tin nhắn |
| `chat.history_window` | 10 | Số tin gần nhất gửi kèm cho mô hình |
| `chat.max_recommendations` | 6 | Số thẻ truyện tối đa mỗi câu trả lời |
| `chat.retention.days` | 90 | Số ngày giữ hội thoại trước khi `ChatCleanupJob` xóa |

## 9. Dữ liệu khởi tạo

- `V2__seed_master_data.sql`: ~30 thể loại (Hành động, Phiêu lưu, Tu tiên, Huyền huyễn, Xuyên không, Trọng sinh, Nữ cường, Ngôn tình, Học đường, Hài hước, Kinh dị, Trinh thám, Đô thị, Cổ đại, Võ hiệp, Khoa học viễn tưởng, Đời thường, Thể thao…) **kèm mô tả một câu** (chatbot dựa vào đó để ánh xạ lời người dùng sang thể loại), bảng `setting`, tài khoản `admin`.
- `db/demo/V100__demo_accounts.sql` (viết tay, đã có): `author1` (AUTHOR, có hồ sơ tác giả) và `reader1` (USER), mật khẩu `Demo@123`.
- `db/demo/V101__demo_content.sql` sinh bằng `backend/scripts/generate-demo-data.mjs` (PRNG cố định seed, tệp SQL sinh ra được commit): ~20 tài khoản (3 tác giả, 1 yêu cầu đang chờ, 1 bị từ chối), ≥ 60 truyện đủ loại/trạng thái/độ dài, ≥ 600 chương, ảnh giữ chỗ tự tạo, bình luận, đánh giá, theo dõi, `story_view_daily` rải 60 ngày (mốc thời gian tương đối so với lúc nạp).

## 10. Truy vấn chính (viết sẵn)

```sql
-- 10.1 Xếp hạng theo lượt xem trong kỳ (:from = hôm nay − 1/7/30 ngày)
SELECT s.id, SUM(v.view_count) AS views
FROM story_view_daily v JOIN story s ON s.id = v.story_id
WHERE v.view_date >= :from AND s.visibility = 'PUBLISHED' AND s.deleted_at IS NULL
GROUP BY s.id ORDER BY views DESC LIMIT :limit;

-- 10.2 Cộng lượt xem theo ngày (nguyên tử, không đọc trước)
INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES (:storyId, :today, 1)
ON DUPLICATE KEY UPDATE view_count = view_count + 1;

-- 10.3 Phát thông báo chương mới cho mọi người theo dõi bằng một câu lệnh
INSERT INTO notification (recipient_id, type, message_args, link)
SELECT f.user_id, 'NEW_CHAPTER', :args, :link FROM story_follow f WHERE f.story_id = :storyId;

-- 10.4 Xuất bản chương hẹn giờ — có điều kiện để job chạy trùng không đăng hai lần
UPDATE chapter SET status = 'PUBLISHED', published_at = :now
WHERE id = :id AND status = 'SCHEDULED' AND scheduled_at <= :now;

-- 10.5 Truyện tương tự: chung nhiều thể loại nhất, cùng loại, đang công khai
SELECT s2.id, COUNT(*) AS shared
FROM story_genre g1 JOIN story_genre g2 ON g2.genre_id = g1.genre_id AND g2.story_id <> g1.story_id
JOIN story s2 ON s2.id = g2.story_id
WHERE g1.story_id = :storyId AND s2.visibility = 'PUBLISHED' AND s2.deleted_at IS NULL
GROUP BY s2.id ORDER BY shared DESC, s2.view_count DESC LIMIT :limit;
```
