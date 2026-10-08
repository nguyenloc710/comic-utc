-- Truyện tranh demo có ảnh thật: 6 tập đầu của webcomic Pepper&Carrot (David Revoy, www.peppercarrot.com),
-- giấy phép Creative Commons Attribution 4.0 — https://creativecommons.org/licenses/by/4.0/
-- Bản dịch tiếng Việt: Binh Pham (kho Deevad/peppercarrot_epNN_translation trên GitHub).
-- Thay đổi so với bản gốc: ghép lớp chữ tiếng Việt vào tranh, thu nhỏ còn rộng 800px, bỏ trang đệm trống.
-- Ảnh nằm ở resources/demo-media/demo/peppercarrot; cách dựng lại ghi ở backend/scripts/peppercarrot/README.md.

INSERT INTO user_account (username, email, password_hash, display_name, role) VALUES
  ('peppercarrot', 'peppercarrot@comic.local', '$2a$12$o5l4khmEgArs.OkS/1sr7OdmAYQNHzfORRAIcLGtsLgz5Gi8Qexdu', 'Pepper&Carrot (CC BY 4.0)', 'AUTHOR');
INSERT INTO author_profile (user_id, pen_name, bio, approved_at)
SELECT id, 'David Revoy', 'Tài khoản demo đăng lại webcomic Pepper&Carrot của họa sĩ David Revoy theo giấy phép CC BY 4.0. Đây không phải tài khoản của tác giả.', NOW(6) - INTERVAL 50000 MINUTE
FROM user_account WHERE username = 'peppercarrot';
SET @u_pc = (SELECT id FROM user_account WHERE username = 'peppercarrot');

INSERT INTO story (slug, title, alt_title, description, cover_path, type, status, visibility, hidden_reason, author_id, chapter_count, view_count, follow_count, rating_sum, rating_count, comment_count, published_at, last_chapter_at, created_at) VALUES
  ('hat-tieu-va-ca-rot', 'Hạt Tiêu & Cà Rốt', 'Pepper&Carrot', 'Hạt Tiêu là cô phù thủy nhỏ sống cùng chú mèo Cà Rốt ở rìa khu rừng của thế giới Hereva. Ngày ngày cô miệt mài pha chế thần dược, và chẳng mẻ thuốc nào diễn ra đúng như dự định.\n\nWebcomic Pepper&Carrot của họa sĩ David Revoy (www.peppercarrot.com), phát hành theo giấy phép Creative Commons Attribution 4.0 (CC BY 4.0). Bản dịch tiếng Việt: Binh Pham. Dữ liệu demo đã được chỉnh sửa: ghép lời thoại vào tranh và thu nhỏ ảnh để hiển thị trên web.', 'demo/peppercarrot/cover.jpg', 'COMIC', 'ONGOING', 'PUBLISHED', NULL, @u_pc, 6, 6722, 0, 0, 0, 0, NOW(6) - INTERVAL 37440 MINUTE, NOW(6) - INTERVAL 1440 MINUTE, NOW(6) - INTERVAL 44640 MINUTE);
SET @s_pc = LAST_INSERT_ID();
INSERT INTO story_genre (story_id, genre_id) SELECT @s_pc, id FROM genre WHERE slug IN ('phieu-luu', 'huyen-huyen', 'hai-huoc');

INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, word_count, view_count) VALUES (@s_pc, 1, 'Thuốc lơ lửng', 'PUBLISHED', NULL, NOW(6) - INTERVAL 37440 MINUTE, 4, 0, 1840);
SET @c_pc = LAST_INSERT_ID();
INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES
  (@c_pc, 1, 'demo/peppercarrot/ep01/page-01.jpg', 800, 214, 14811),
  (@c_pc, 2, 'demo/peppercarrot/ep01/page-02.jpg', 800, 1130, 119261),
  (@c_pc, 3, 'demo/peppercarrot/ep01/page-03.jpg', 800, 1130, 118681),
  (@c_pc, 4, 'demo/peppercarrot/ep01/page-04.jpg', 800, 1130, 86529);

INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, word_count, view_count) VALUES (@s_pc, 2, 'Thuốc cầu vồng', 'PUBLISHED', NULL, NOW(6) - INTERVAL 30240 MINUTE, 7, 0, 1325);
SET @c_pc = LAST_INSERT_ID();
INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES
  (@c_pc, 1, 'demo/peppercarrot/ep02/page-01.jpg', 800, 214, 15356),
  (@c_pc, 2, 'demo/peppercarrot/ep02/page-02.jpg', 800, 1130, 162731),
  (@c_pc, 3, 'demo/peppercarrot/ep02/page-03.jpg', 800, 1130, 142735),
  (@c_pc, 4, 'demo/peppercarrot/ep02/page-04.jpg', 800, 1130, 130188),
  (@c_pc, 5, 'demo/peppercarrot/ep02/page-05.jpg', 800, 1130, 134111),
  (@c_pc, 6, 'demo/peppercarrot/ep02/page-06.jpg', 800, 1130, 114745),
  (@c_pc, 7, 'demo/peppercarrot/ep02/page-07.jpg', 800, 290, 47920);

INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, word_count, view_count) VALUES (@s_pc, 3, 'Nguyên liệu bí mật', 'PUBLISHED', NULL, NOW(6) - INTERVAL 23040 MINUTE, 9, 0, 1112);
SET @c_pc = LAST_INSERT_ID();
INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES
  (@c_pc, 1, 'demo/peppercarrot/ep03/page-01.jpg', 800, 214, 16815),
  (@c_pc, 2, 'demo/peppercarrot/ep03/page-02.jpg', 800, 1130, 214785),
  (@c_pc, 3, 'demo/peppercarrot/ep03/page-03.jpg', 800, 1130, 193508),
  (@c_pc, 4, 'demo/peppercarrot/ep03/page-04.jpg', 800, 1130, 118963),
  (@c_pc, 5, 'demo/peppercarrot/ep03/page-05.jpg', 800, 1130, 146402),
  (@c_pc, 6, 'demo/peppercarrot/ep03/page-06.jpg', 800, 1130, 186982),
  (@c_pc, 7, 'demo/peppercarrot/ep03/page-07.jpg', 800, 1130, 168630),
  (@c_pc, 8, 'demo/peppercarrot/ep03/page-08.jpg', 800, 1130, 153307),
  (@c_pc, 9, 'demo/peppercarrot/ep03/page-09.jpg', 800, 658, 147607);

INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, word_count, view_count) VALUES (@s_pc, 4, 'Khoảnh khắc thiên tài', 'PUBLISHED', NULL, NOW(6) - INTERVAL 15840 MINUTE, 9, 0, 954);
SET @c_pc = LAST_INSERT_ID();
INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES
  (@c_pc, 1, 'demo/peppercarrot/ep04/page-01.jpg', 800, 210, 14341),
  (@c_pc, 2, 'demo/peppercarrot/ep04/page-02.jpg', 800, 1130, 142564),
  (@c_pc, 3, 'demo/peppercarrot/ep04/page-03.jpg', 800, 1130, 167426),
  (@c_pc, 4, 'demo/peppercarrot/ep04/page-04.jpg', 800, 1130, 160510),
  (@c_pc, 5, 'demo/peppercarrot/ep04/page-05.jpg', 800, 209, 19834),
  (@c_pc, 6, 'demo/peppercarrot/ep04/page-06.jpg', 800, 450, 31143),
  (@c_pc, 7, 'demo/peppercarrot/ep04/page-07.jpg', 800, 507, 72806),
  (@c_pc, 8, 'demo/peppercarrot/ep04/page-08.jpg', 800, 1130, 171483),
  (@c_pc, 9, 'demo/peppercarrot/ep04/page-09.jpg', 800, 799, 188993);

INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, word_count, view_count) VALUES (@s_pc, 5, 'Giáng sinh, tập đặc biệt', 'PUBLISHED', NULL, NOW(6) - INTERVAL 8640 MINUTE, 6, 0, 803);
SET @c_pc = LAST_INSERT_ID();
INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES
  (@c_pc, 1, 'demo/peppercarrot/ep05/page-01.jpg', 800, 214, 13337),
  (@c_pc, 2, 'demo/peppercarrot/ep05/page-02.jpg', 800, 1130, 111312),
  (@c_pc, 3, 'demo/peppercarrot/ep05/page-03.jpg', 800, 1130, 97379),
  (@c_pc, 4, 'demo/peppercarrot/ep05/page-04.jpg', 800, 1130, 108899),
  (@c_pc, 5, 'demo/peppercarrot/ep05/page-05.jpg', 800, 1130, 58348),
  (@c_pc, 6, 'demo/peppercarrot/ep05/page-06.jpg', 800, 799, 178592);

INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, word_count, view_count) VALUES (@s_pc, 6, 'Cuộc thi thần dược', 'PUBLISHED', NULL, NOW(6) - INTERVAL 1440 MINUTE, 11, 0, 688);
SET @c_pc = LAST_INSERT_ID();
INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES
  (@c_pc, 1, 'demo/peppercarrot/ep06/page-01.jpg', 800, 214, 15562),
  (@c_pc, 2, 'demo/peppercarrot/ep06/page-02.jpg', 800, 1130, 131120),
  (@c_pc, 3, 'demo/peppercarrot/ep06/page-03.jpg', 800, 1130, 123791),
  (@c_pc, 4, 'demo/peppercarrot/ep06/page-04.jpg', 800, 1130, 141672),
  (@c_pc, 5, 'demo/peppercarrot/ep06/page-05.jpg', 800, 1130, 200383),
  (@c_pc, 6, 'demo/peppercarrot/ep06/page-06.jpg', 800, 1130, 193293),
  (@c_pc, 7, 'demo/peppercarrot/ep06/page-07.jpg', 800, 1130, 193893),
  (@c_pc, 8, 'demo/peppercarrot/ep06/page-08.jpg', 800, 1130, 193439),
  (@c_pc, 9, 'demo/peppercarrot/ep06/page-09.jpg', 800, 1130, 160431),
  (@c_pc, 10, 'demo/peppercarrot/ep06/page-10.jpg', 800, 1130, 174637),
  (@c_pc, 11, 'demo/peppercarrot/ep06/page-11.jpg', 800, 799, 224472);

-- Lượt xem theo ngày để truyện có mặt trong bảng xếp hạng tuần / tháng
INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 0 DAY, 212),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 1 DAY, 188),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 2 DAY, 175),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 3 DAY, 160),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 4 DAY, 149),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 5 DAY, 151),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 6 DAY, 137),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 7 DAY, 120),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 8 DAY, 118),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 9 DAY, 104),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 10 DAY, 96),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 11 DAY, 90),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 12 DAY, 84),
  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL 13 DAY, 80);
