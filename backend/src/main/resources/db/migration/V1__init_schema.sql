-- Lược đồ khởi tạo. Đặc tả từng bảng: docs/01-MO-HINH-DU-LIEU.md.
-- Thời gian lưu UTC (DATETIME(6)); enum lưu VARCHAR + CHECK; bộ đếm *_count chỉ cập nhật bằng SET x = x + :delta.

-- =====================================================================
-- Người dùng & tác giả
-- =====================================================================
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
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)  NULL,
  created_by      BIGINT       NULL,
  updated_by      BIGINT       NULL,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_email (email),
  CONSTRAINT ck_user_role   CHECK (role IN ('USER', 'AUTHOR', 'ADMIN')),
  CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE', 'BANNED'))
);

CREATE TABLE author_profile (
  user_id     BIGINT        PRIMARY KEY,
  pen_name    VARCHAR(100)  NOT NULL,
  bio         VARCHAR(1000) NULL,
  approved_at DATETIME(6)   NOT NULL,
  UNIQUE KEY uk_author_profile_pen_name (pen_name),
  CONSTRAINT fk_author_profile_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE TABLE author_request (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT        NOT NULL,
  pen_name        VARCHAR(100)  NOT NULL,
  introduction    VARCHAR(2000) NOT NULL,
  intended_type   VARCHAR(20)   NOT NULL,
  status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  reject_reason   VARCHAR(1000) NULL,
  reviewed_by     BIGINT        NULL,
  reviewed_at     DATETIME(6)   NULL,
  created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)   NULL,
  created_by      BIGINT        NULL,
  updated_by      BIGINT        NULL,
  -- MySQL không có unique index có điều kiện: cột sinh này chỉ khác NULL khi đang chờ duyệt,
  -- nên UNIQUE trên nó đảm bảo mỗi người tối đa một yêu cầu PENDING ngay ở tầng DB.
  pending_user_id BIGINT GENERATED ALWAYS AS (IF(status = 'PENDING', user_id, NULL)) STORED,
  UNIQUE KEY uk_author_request_pending (pending_user_id),
  INDEX idx_author_request_status (status, created_at),
  INDEX idx_author_request_user (user_id),
  CONSTRAINT fk_author_request_user     FOREIGN KEY (user_id)     REFERENCES user_account (id),
  CONSTRAINT fk_author_request_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account (id),
  CONSTRAINT ck_author_request_type   CHECK (intended_type IN ('COMIC', 'NOVEL', 'BOTH')),
  CONSTRAINT ck_author_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- =====================================================================
-- Thể loại & truyện
-- =====================================================================
CREATE TABLE genre (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  slug        VARCHAR(120) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  is_active   TINYINT(1)   NOT NULL DEFAULT 1,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6)  NULL,
  created_by  BIGINT       NULL,
  updated_by  BIGINT       NULL,
  UNIQUE KEY uk_genre_name (name),
  UNIQUE KEY uk_genre_slug (slug)
);

CREATE TABLE story (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  slug            VARCHAR(220)  NOT NULL,
  title           VARCHAR(200)  NOT NULL,
  alt_title       VARCHAR(200)  NULL,
  description     TEXT          NOT NULL,
  cover_path      VARCHAR(500)  NULL,
  type            VARCHAR(10)   NOT NULL,
  status          VARCHAR(20)   NOT NULL DEFAULT 'ONGOING',
  visibility      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
  hidden_reason   VARCHAR(1000) NULL,
  author_id       BIGINT        NOT NULL,
  chapter_count   INT           NOT NULL DEFAULT 0,
  view_count      BIGINT        NOT NULL DEFAULT 0,
  follow_count    INT           NOT NULL DEFAULT 0,
  rating_sum      INT           NOT NULL DEFAULT 0,
  rating_count    INT           NOT NULL DEFAULT 0,
  comment_count   INT           NOT NULL DEFAULT 0,
  published_at    DATETIME(6)   NULL,
  last_chapter_at DATETIME(6)   NULL,
  deleted_at      DATETIME(6)   NULL,
  version         INT           NOT NULL DEFAULT 0,
  created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at      DATETIME(6)   NULL,
  created_by      BIGINT        NULL,
  updated_by      BIGINT        NULL,
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

-- =====================================================================
-- Chương
-- =====================================================================
CREATE TABLE chapter (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  story_id      BIGINT        NOT NULL,
  chapter_no    INT           NOT NULL,
  title         VARCHAR(200)  NULL,
  status        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
  scheduled_at  DATETIME(6)   NULL,
  published_at  DATETIME(6)   NULL,
  hidden_reason VARCHAR(1000) NULL,
  page_count    INT           NOT NULL DEFAULT 0,
  word_count    INT           NOT NULL DEFAULT 0,
  view_count    BIGINT        NOT NULL DEFAULT 0,
  version       INT           NOT NULL DEFAULT 0,
  created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    DATETIME(6)   NULL,
  created_by    BIGINT        NULL,
  updated_by    BIGINT        NULL,
  UNIQUE KEY uk_chapter_story_no (story_id, chapter_no),
  INDEX idx_chapter_story_status (story_id, status, chapter_no),
  INDEX idx_chapter_scheduled (status, scheduled_at),
  CONSTRAINT fk_chapter_story FOREIGN KEY (story_id) REFERENCES story (id),
  CONSTRAINT ck_chapter_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'HIDDEN')),
  CONSTRAINT ck_chapter_no CHECK (chapter_no > 0)
);

-- Tách khỏi chapter để danh sách chương và trang quản lý không kéo LONGTEXT
CREATE TABLE chapter_content (
  chapter_id BIGINT   PRIMARY KEY,
  content    LONGTEXT NOT NULL,
  CONSTRAINT fk_chapter_content_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id)
);

-- Không đặt UNIQUE (chapter_id, page_no): đổi thứ tự là cập nhật hàng loạt page_no,
-- ràng buộc duy nhất sẽ vấp giá trị trung gian. Thứ tự đúng do service đảm bảo.
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

-- =====================================================================
-- Tương tác của độc giả
-- =====================================================================
CREATE TABLE story_follow (
  user_id    BIGINT      NOT NULL,
  story_id   BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, story_id),
  INDEX idx_story_follow_story (story_id),
  CONSTRAINT fk_story_follow_user  FOREIGN KEY (user_id)  REFERENCES user_account (id),
  CONSTRAINT fk_story_follow_story FOREIGN KEY (story_id) REFERENCES story (id)
);

CREATE TABLE reading_history (
  user_id    BIGINT      NOT NULL,
  story_id   BIGINT      NOT NULL,
  chapter_id BIGINT      NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id, story_id),
  INDEX idx_reading_history_recent (user_id, updated_at),
  CONSTRAINT fk_reading_history_user    FOREIGN KEY (user_id)    REFERENCES user_account (id),
  CONSTRAINT fk_reading_history_story   FOREIGN KEY (story_id)   REFERENCES story (id),
  CONSTRAINT fk_reading_history_chapter FOREIGN KEY (chapter_id) REFERENCES chapter (id)
);

CREATE TABLE story_rating (
  user_id    BIGINT      NOT NULL,
  story_id   BIGINT      NOT NULL,
  stars      TINYINT     NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id, story_id),
  INDEX idx_story_rating_story (story_id),
  CONSTRAINT fk_story_rating_user  FOREIGN KEY (user_id)  REFERENCES user_account (id),
  CONSTRAINT fk_story_rating_story FOREIGN KEY (story_id) REFERENCES story (id),
  CONSTRAINT ck_story_rating_stars CHECK (stars BETWEEN 1 AND 5)
);

CREATE TABLE comment (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  story_id      BIGINT        NOT NULL,
  chapter_id    BIGINT        NULL,
  user_id       BIGINT        NOT NULL,
  parent_id     BIGINT        NULL,
  content       VARCHAR(1000) NOT NULL,
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
  target_id       BIGINT        NOT NULL,
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

-- =====================================================================
-- Thông báo & thống kê
-- =====================================================================
CREATE TABLE notification (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_id BIGINT       NOT NULL,
  type         VARCHAR(40)  NOT NULL,
  message_args JSON         NULL,
  link         VARCHAR(500) NULL,
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
  view_date  DATE   NOT NULL,
  view_count INT    NOT NULL DEFAULT 0,
  PRIMARY KEY (story_id, view_date),
  INDEX idx_story_view_daily_date (view_date, view_count),
  CONSTRAINT fk_story_view_daily_story FOREIGN KEY (story_id) REFERENCES story (id)
);

-- =====================================================================
-- Hệ thống
-- =====================================================================
CREATE TABLE setting (
  setting_key   VARCHAR(100) PRIMARY KEY,
  setting_value TEXT         NOT NULL,
  value_type    VARCHAR(20)  NOT NULL DEFAULT 'STRING',
  group_name    VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
  description   VARCHAR(500) NULL,
  is_read_only  TINYINT(1)   NOT NULL DEFAULT 0,
  updated_at    DATETIME(6)  NULL,
  updated_by    BIGINT       NULL,
  CONSTRAINT ck_setting_value_type CHECK (value_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON'))
);

-- Không có FK tới user_account để nhật ký không bao giờ chặn thao tác trên tài khoản
CREATE TABLE audit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id    BIGINT       NULL,
  actor_name  VARCHAR(50)  NULL,
  action      VARCHAR(40)  NOT NULL,
  entity_type VARCHAR(50)  NULL,
  entity_id   VARCHAR(50)  NULL,
  detail      JSON         NULL,
  ip_address  VARCHAR(45)  NULL,
  user_agent  VARCHAR(255) NULL,
  request_id  VARCHAR(36)  NULL,
  created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_audit_actor  (actor_id, created_at),
  INDEX idx_audit_action (action, created_at),
  INDEX idx_audit_entity (entity_type, entity_id)
);

CREATE TABLE job_run (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_name        VARCHAR(50)   NOT NULL,
  started_at      DATETIME(6)   NOT NULL,
  finished_at     DATETIME(6)   NULL,
  status          VARCHAR(10)   NOT NULL,
  processed_count INT           NOT NULL DEFAULT 0,
  error_count     INT           NOT NULL DEFAULT 0,
  error_message   VARCHAR(1000) NULL,
  INDEX idx_job_run_name (job_name, started_at),
  CONSTRAINT ck_job_run_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);
