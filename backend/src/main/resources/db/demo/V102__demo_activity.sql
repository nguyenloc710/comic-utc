-- =====================================================================
-- Dữ liệu demo cho các chức năng giai đoạn 4–6 (chỉ nạp ở profile dev):
-- yêu cầu làm tác giả, thông báo, báo cáo vi phạm, chương hẹn giờ, hội thoại chatbot, nhật ký, lượt chạy job.
-- Tra id theo tên đăng nhập / slug để không phụ thuộc thứ tự id của V100–V101.
-- Mốc thời gian tương đối so với lúc nạp (NOW() là UTC vì MySQL chạy --default-time-zone=+00:00).
-- Câu trả lời "LLM" trong các hội thoại dưới đây là MẪU viết tay để minh họa giao diện, không phải mô hình sinh ra.
-- =====================================================================

SET @admin   := (SELECT id FROM user_account WHERE username = 'admin');
SET @author1 := (SELECT id FROM user_account WHERE username = 'author1');
SET @author3 := (SELECT id FROM user_account WHERE username = 'author3');
SET @reader1 := (SELECT id FROM user_account WHERE username = 'reader1');
SET @reader2 := (SELECT id FROM user_account WHERE username = 'reader2');
SET @reader3 := (SELECT id FROM user_account WHERE username = 'reader3');
SET @reader4 := (SELECT id FROM user_account WHERE username = 'reader4');
SET @reader5 := (SELECT id FROM user_account WHERE username = 'reader5');
SET @reader6 := (SELECT id FROM user_account WHERE username = 'reader6');
SET @reader7 := (SELECT id FROM user_account WHERE username = 'reader7');
SET @reader8 := (SELECT id FROM user_account WHERE username = 'reader8');

SET @s_tinh_ha    := (SELECT id FROM story WHERE slug = 'tinh-ha-dao-quan');
SET @s_huyen_thien:= (SELECT id FROM story WHERE slug = 'huyen-thien-kiem-ton');
SET @s_thanh_van  := (SELECT id FROM story WHERE slug = 'thanh-van-kiem-ton');
SET @s_tieng_go   := (SELECT id FROM story WHERE slug = 'tieng-go-so-13');
SET @s_chuyen_tau := (SELECT id FROM story WHERE slug = 'chuyen-tau-so-13');
SET @s_hanh_lang  := (SELECT id FROM story WHERE slug = 'tieng-go-cuoi-hanh-lang');
SET @s_ho_so      := (SELECT id FROM story WHERE slug = 'ho-so-khong-loi-giai');
SET @s_vu_an      := (SELECT id FROM story WHERE slug = 'vu-an-mat-so-7');
SET @s_nguoi_choi := (SELECT id FROM story WHERE slug = 'nguoi-choi-toi-thuong');
SET @s_thai_hu    := (SELECT id FROM story WHERE slug = 'thai-hu-kiem-ton');
SET @s_hidden     := (SELECT id FROM story WHERE slug = 'tinh-ha-than-vuong');
SET @s_ban_cuoi   := (SELECT id FROM story WHERE slug = 'ban-cuoi-ba-dao');

-- ---------------------------------------------------------------------
-- Yêu cầu làm tác giả: hai đang chờ duyệt, một đã bị từ chối (đã qua thời gian chờ nên gửi lại được)
-- ---------------------------------------------------------------------
INSERT INTO author_request (user_id, pen_name, introduction, intended_type, status, reject_reason, reviewed_by,
                            reviewed_at, created_at) VALUES
  (@reader2, 'Bút Mực Xanh',
   'Mình viết truyện ngắn trên blog cá nhân được hai năm, chủ yếu thể loại đời thường và học đường. Muốn đăng một bộ truyện chữ dài khoảng 40 chương về câu lạc bộ văn học ở trường cấp ba.',
   'NOVEL', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 2 DAY),
  (@reader4, 'Mèo Vẽ Tranh',
   'Mình là sinh viên thiết kế, vẽ truyện tranh bốn khung hài hước về cuộc sống ký túc xá. Đã có sẵn 12 chương, mỗi chương 8–10 trang.',
   'COMIC', 'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 5 HOUR),
  (@reader3, 'Trang Giấy Trắng',
   'Mình thích đọc ngôn tình và muốn thử viết.',
   'NOVEL', 'REJECTED',
   'Lời giới thiệu còn quá ngắn, chưa cho thấy kinh nghiệm hay dự định sáng tác cụ thể. Bạn bổ sung đề cương truyện và một chương viết thử rồi gửi lại nhé.',
   @admin, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 12 DAY);

-- ---------------------------------------------------------------------
-- Chương hẹn giờ của author1 (hiện trong studio với nhãn "Hẹn giờ"; tới giờ thì job tự đăng)
-- ---------------------------------------------------------------------
SET @next_no := (SELECT MAX(chapter_no) + 1 FROM chapter WHERE story_id = @s_thai_hu);
INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, word_count, created_by, created_at)
VALUES (@s_thai_hu, @next_no, 'Kiếm ý thức tỉnh', 'SCHEDULED', NOW() + INTERVAL 2 DAY, 42, @author1, NOW() - INTERVAL 1 HOUR);
INSERT INTO chapter_content (chapter_id, content) VALUES (LAST_INSERT_ID(),
  '<h2>Kiếm ý thức tỉnh</h2><p>Gió trên đỉnh Thái Hư thổi suốt đêm. Thanh kiếm cũ trong tay hắn bỗng rung lên, như đáp lại một tiếng gọi rất xa.</p><p>Hắn nhắm mắt, lần đầu tiên nghe thấy kiếm ý của chính mình.</p>');

-- ---------------------------------------------------------------------
-- Thông báo
-- ---------------------------------------------------------------------
INSERT INTO notification (recipient_id, type, message_args, link, is_read, created_at) VALUES
  (@reader1, 'NEW_CHAPTER', JSON_ARRAY((SELECT title FROM story WHERE id = @s_tinh_ha), '25'),
   '/stories/tinh-ha-dao-quan/chapters/25', 0, NOW() - INTERVAL 3 HOUR),
  (@reader1, 'NEW_CHAPTER', JSON_ARRAY((SELECT title FROM story WHERE id = @s_huyen_thien), '13'),
   '/stories/huyen-thien-kiem-ton/chapters/13', 0, NOW() - INTERVAL 20 HOUR),
  (@reader1, 'COMMENT_REPLIED', JSON_ARRAY('Quang Huy', (SELECT title FROM story WHERE id = @s_ban_cuoi)),
   '/stories/ban-cuoi-ba-dao#comments', 0, NOW() - INTERVAL 1 DAY),
  (@reader1, 'NEW_CHAPTER', JSON_ARRAY((SELECT title FROM story WHERE id = @s_ban_cuoi), '15'),
   '/stories/ban-cuoi-ba-dao/chapters/15', 1, NOW() - INTERVAL 4 DAY),
  (@author1, 'CONTENT_HIDDEN', JSON_ARRAY((SELECT title FROM story WHERE id = @s_hidden),
   (SELECT hidden_reason FROM story WHERE id = @s_hidden)), '/studio/stories', 0, NOW() - INTERVAL 6 DAY),
  (@reader3, 'AUTHOR_REQUEST_REJECTED',
   JSON_ARRAY('Lời giới thiệu còn quá ngắn, chưa cho thấy kinh nghiệm hay dự định sáng tác cụ thể. Bạn bổ sung đề cương truyện và một chương viết thử rồi gửi lại nhé.'),
   '/me/author-request', 1, NOW() - INTERVAL 10 DAY);

-- ---------------------------------------------------------------------
-- Báo cáo vi phạm: ba đang chờ (truyện, chương, bình luận), một đã bỏ qua
-- ---------------------------------------------------------------------
SET @comment_spam := (SELECT MIN(id) FROM comment WHERE parent_id IS NULL AND status = 'VISIBLE');
SET @chapter_report := (SELECT id FROM chapter WHERE story_id = @s_huyen_thien AND chapter_no = 3);
INSERT INTO report (reporter_id, target_type, target_id, reason, detail, status, handled_by, handled_at,
                    resolution_note, created_at) VALUES
  (@reader5, 'STORY', @s_nguoi_choi, 'COPYRIGHT',
   'Cốt truyện và tên nhân vật giống hệt một bộ truyện mạng nổi tiếng, chỉ đổi bối cảnh.', 'PENDING',
   NULL, NULL, NULL, NOW() - INTERVAL 7 HOUR),
  (@reader6, 'COMMENT', @comment_spam, 'SPAM', 'Bình luận lặp đi lặp lại ở nhiều truyện.', 'PENDING',
   NULL, NULL, NULL, NOW() - INTERVAL 2 HOUR),
  (@reader7, 'CHAPTER', @chapter_report, 'VIOLENCE', 'Trang 4–6 có cảnh máu me khá nặng, nên cảnh báo người đọc.',
   'PENDING', NULL, NULL, NULL, NOW() - INTERVAL 1 DAY),
  (@reader8, 'STORY', @s_ho_so, 'OTHER', 'Truyện ra chậm quá.', 'DISMISSED', @admin, NOW() - INTERVAL 3 DAY,
   'Không phải vi phạm nội dung; tốc độ ra chương do tác giả quyết định.', NOW() - INTERVAL 4 DAY);

-- ---------------------------------------------------------------------
-- Hội thoại chatbot của reader1 (lời trợ lý là MẪU minh họa) và một lượt rơi về đường lui của reader2
-- ---------------------------------------------------------------------
-- Hội thoại 1: hỏi nối tiếp
INSERT INTO chat_conversation (user_id, title, created_at, last_message_at)
VALUES (@reader1, 'Truyện tu tiên đã hoàn thành', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 3 MINUTE);
SET @c1 := LAST_INSERT_ID();
INSERT INTO chat_message (conversation_id, role, content, created_at)
VALUES (@c1, 'USER', 'Truyện tu tiên đã hoàn thành', NOW() - INTERVAL 2 DAY);
INSERT INTO chat_message (conversation_id, role, content, recommendations, tool_trace, answer_source, rejected_count,
                          prompt_tokens, completion_tokens, latency_ms, feedback, created_at)
VALUES (@c1, 'ASSISTANT',
  'Kho hiện có một bộ tu tiên đã hoàn thành rất được yêu thích, bạn đọc một mạch được luôn nhé.',
  JSON_ARRAY(JSON_OBJECT('storyId', @s_tinh_ha, 'title', (SELECT title FROM story WHERE id = @s_tinh_ha),
    'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_tinh_ha),
    'reason', 'Tu tiên kết hợp huyền huyễn, đã hoàn thành 25 chương và đang đứng đầu lượt xem.')),
  JSON_ARRAY(JSON_OBJECT('tool', 'searchStories',
    'arguments', JSON_OBJECT('genreSlugs', JSON_ARRAY('tu-tien'), 'status', 'COMPLETED'),
    'storyIds', JSON_ARRAY(@s_tinh_ha))),
  'LLM', 0, 2860, 142, 2310, 1, NOW() - INTERVAL 2 DAY + INTERVAL 5 SECOND);
INSERT INTO chat_message (conversation_id, role, content, created_at)
VALUES (@c1, 'USER', 'Có truyện tranh nào cùng thể loại không?', NOW() - INTERVAL 2 DAY + INTERVAL 3 MINUTE);
INSERT INTO chat_message (conversation_id, role, content, recommendations, tool_trace, answer_source, rejected_count,
                          prompt_tokens, completion_tokens, latency_ms, feedback, created_at)
VALUES (@c1, 'ASSISTANT',
  'Truyện tranh tu tiên thì chưa có bộ nào hoàn thành, đây là hai bộ đang ra mà bạn có thể theo dõi:',
  JSON_ARRAY(
    JSON_OBJECT('storyId', @s_huyen_thien, 'title', (SELECT title FROM story WHERE id = @s_huyen_thien),
      'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_huyen_thien),
      'reason', 'Truyện tranh tu tiên nhiều cảnh hành động, nét vẽ đẹp.'),
    JSON_OBJECT('storyId', @s_thanh_van, 'title', (SELECT title FROM story WHERE id = @s_thanh_van),
      'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_thanh_van),
      'reason', 'Mới 8 chương, hợp để bắt đầu theo dõi từ sớm.')),
  JSON_ARRAY(JSON_OBJECT('tool', 'searchStories',
    'arguments', JSON_OBJECT('genreSlugs', JSON_ARRAY('tu-tien'), 'type', 'COMIC', 'status', 'COMPLETED'),
    'storyIds', JSON_ARRAY(@s_huyen_thien, @s_thanh_van))),
  'LLM', 0, 3410, 168, 2780, NULL, NOW() - INTERVAL 2 DAY + INTERVAL 3 MINUTE + INTERVAL 6 SECOND);

-- Hội thoại 2: kinh dị
INSERT INTO chat_conversation (user_id, title, created_at, last_message_at)
VALUES (@reader1, 'Truyện kinh dị đọc ban đêm cho rợn', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY);
SET @c2 := LAST_INSERT_ID();
INSERT INTO chat_message (conversation_id, role, content, created_at)
VALUES (@c2, 'USER', 'Truyện kinh dị đọc ban đêm cho rợn', NOW() - INTERVAL 1 DAY);
INSERT INTO chat_message (conversation_id, role, content, recommendations, tool_trace, answer_source, rejected_count,
                          prompt_tokens, completion_tokens, latency_ms, feedback, created_at)
VALUES (@c2, 'ASSISTANT', 'Ba bộ kinh dị – bí ẩn này đọc lúc khuya là "đủ đô" đấy:',
  JSON_ARRAY(
    JSON_OBJECT('storyId', @s_tieng_go, 'title', (SELECT title FROM story WHERE id = @s_tieng_go),
      'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_tieng_go),
      'reason', 'Truyện tranh, đã hoàn thành, không khí ngột ngạt từ chương đầu.'),
    JSON_OBJECT('storyId', @s_chuyen_tau, 'title', (SELECT title FROM story WHERE id = @s_chuyen_tau),
      'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_chuyen_tau),
      'reason', 'Truyện chữ đã hoàn thành, nhiều cú lật ở cuối.'),
    JSON_OBJECT('storyId', @s_hanh_lang, 'title', (SELECT title FROM story WHERE id = @s_hanh_lang),
      'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_hanh_lang),
      'reason', 'Đang ra, hợp nếu bạn thích đọc theo từng chương mới.')),
  JSON_ARRAY(JSON_OBJECT('tool', 'searchStories', 'arguments', JSON_OBJECT('genreSlugs', JSON_ARRAY('kinh-di')),
    'storyIds', JSON_ARRAY(@s_tieng_go, @s_chuyen_tau, @s_hanh_lang))),
  'LLM', 1, 2950, 201, 3120, 1, NOW() - INTERVAL 1 DAY + INTERVAL 7 SECOND);

-- Hội thoại 3: ngoài phạm vi → từ chối, không gọi hàm
INSERT INTO chat_conversation (user_id, title, created_at, last_message_at)
VALUES (@reader1, 'Viết hộ tôi bài văn tả mẹ', NOW() - INTERVAL 30 MINUTE, NOW() - INTERVAL 30 MINUTE);
SET @c3 := LAST_INSERT_ID();
INSERT INTO chat_message (conversation_id, role, content, created_at)
VALUES (@c3, 'USER', 'Viết hộ tôi bài văn tả mẹ', NOW() - INTERVAL 30 MINUTE);
INSERT INTO chat_message (conversation_id, role, content, recommendations, answer_source, rejected_count,
                          prompt_tokens, completion_tokens, latency_ms, created_at)
VALUES (@c3, 'ASSISTANT',
  'Mình chỉ giúp tìm và gợi ý truyện trên Comic UTC thôi. Bạn muốn đọc truyện về tình cảm gia đình không? Mình gợi ý vài bộ nhé.',
  JSON_ARRAY(), 'LLM', 0, 2610, 64, 1480, NOW() - INTERVAL 30 MINUTE + INTERVAL 3 SECOND);

-- Hội thoại của reader2: mô hình quá thời gian chờ → đường lui; một câu trả lời bị chê để trang quản trị có dữ liệu
INSERT INTO chat_conversation (user_id, title, created_at, last_message_at)
VALUES (@reader2, 'Truyện trinh thám phá án', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR);
SET @c4 := LAST_INSERT_ID();
INSERT INTO chat_message (conversation_id, role, content, created_at)
VALUES (@c4, 'USER', 'Truyện trinh thám phá án', NOW() - INTERVAL 6 HOUR);
INSERT INTO chat_message (conversation_id, role, content, recommendations, answer_source, fallback_reason,
                          rejected_count, latency_ms, feedback, created_at)
VALUES (@c4, 'ASSISTANT',
  'Trợ lý đang bận một chút. Đây là các truyện tìm theo từ khóa trong câu hỏi của bạn:',
  JSON_ARRAY(
    JSON_OBJECT('storyId', @s_ho_so, 'title', (SELECT title FROM story WHERE id = @s_ho_so),
      'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_ho_so), 'reason', NULL),
    JSON_OBJECT('storyId', @s_vu_an, 'title', (SELECT title FROM story WHERE id = @s_vu_an),
      'chapterCount', (SELECT chapter_count FROM story WHERE id = @s_vu_an), 'reason', NULL)),
  'FALLBACK_KEYWORD', 'TIMEOUT', 0, 30120, -1, NOW() - INTERVAL 6 HOUR + INTERVAL 31 SECOND);

-- ---------------------------------------------------------------------
-- Nhật ký kiểm toán và lượt chạy job
-- ---------------------------------------------------------------------
INSERT INTO audit_log (actor_id, actor_name, action, entity_type, entity_id, detail, ip_address, created_at) VALUES
  (@admin, 'admin', 'AUTHOR_REQUEST_REJECTED', 'AuthorRequest',
   (SELECT CAST(id AS CHAR) FROM author_request WHERE user_id = @reader3),
   JSON_OBJECT('userId', @reader3, 'penName', 'Trang Giấy Trắng'), '127.0.0.1', NOW() - INTERVAL 10 DAY),
  (@admin, 'admin', 'STORY_HIDDEN', 'Story', CAST(@s_hidden AS CHAR),
   JSON_OBJECT('title', (SELECT title FROM story WHERE id = @s_hidden), 'reason',
     (SELECT hidden_reason FROM story WHERE id = @s_hidden)), '127.0.0.1', NOW() - INTERVAL 6 DAY),
  (@admin, 'admin', 'REPORT_DISMISSED', 'Report',
   (SELECT CAST(MAX(id) AS CHAR) FROM report WHERE status = 'DISMISSED'),
   JSON_OBJECT('target', CONCAT('STORY:', @s_ho_so)), '127.0.0.1', NOW() - INTERVAL 3 DAY),
  (@admin, 'admin', 'SETTING_CHANGED', 'Setting', 'comment.cooldown.seconds',
   JSON_OBJECT('oldValue', '30', 'newValue', '15'), '127.0.0.1', NOW() - INTERVAL 8 DAY);

INSERT INTO job_run (job_name, started_at, finished_at, status, processed_count, error_count) VALUES
  ('chapter-publish', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 1 SECOND, 'SUCCESS', 2, 0),
  ('chapter-publish', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 1 SECOND, 'SUCCESS', 1, 0);
