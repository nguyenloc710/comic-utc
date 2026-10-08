-- =====================================================================
-- Chatbot gợi ý truyện: hội thoại và tin nhắn (docs/01 §7, docs/04)
-- =====================================================================
CREATE TABLE chat_conversation (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT       NOT NULL,
  title           VARCHAR(200) NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_message_at DATETIME(6)  NOT NULL,
  INDEX idx_chat_conversation_user (user_id, last_message_at),
  INDEX idx_chat_conversation_last (last_message_at),
  CONSTRAINT fk_chat_conversation_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE TABLE chat_message (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id   BIGINT      NOT NULL,
  role              VARCHAR(20) NOT NULL,
  content           TEXT        NOT NULL,
  -- [{storyId, title, chapterCount, reason}] ĐÃ hậu kiểm; tên và số chương chép lại để tóm tắt cho mô hình ở lượt sau
  recommendations   JSON        NULL,
  -- [{tool, arguments, storyIds}]: để hỏi nối tiếp, để hậu kiểm lượt sau và để chẩn đoán
  tool_trace        JSON        NULL,
  answer_source     VARCHAR(20) NULL,
  fallback_reason   VARCHAR(50) NULL,
  -- Số gợi ý của mô hình bị hậu kiểm loại (id không đến từ kết quả hàm hoặc truyện không còn công khai)
  rejected_count    INT         NULL,
  prompt_tokens     INT         NULL,
  completion_tokens INT         NULL,
  latency_ms        INT         NULL,
  feedback          TINYINT     NULL,
  created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_chat_message_conversation (conversation_id, id),
  INDEX idx_chat_message_created (created_at, role),
  CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation (id),
  CONSTRAINT ck_chat_message_role CHECK (role IN ('USER', 'ASSISTANT')),
  CONSTRAINT ck_chat_message_source CHECK (answer_source IS NULL OR answer_source IN ('LLM', 'FALLBACK_KEYWORD')),
  CONSTRAINT ck_chat_message_feedback CHECK (feedback IS NULL OR feedback IN (-1, 1))
);
