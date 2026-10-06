-- Tài khoản demo cho môi trường dev (chỉ nạp ở profile dev). Mật khẩu chung: Demo@123 (BCrypt cost 12).
-- Truyện, chương và tương tác mẫu sẽ nằm ở V101 trở đi (giai đoạn 3).
INSERT INTO user_account (username, email, password_hash, display_name, role) VALUES
  ('author1', 'author1@comic.local', '$2a$12$o5l4khmEgArs.OkS/1sr7OdmAYQNHzfORRAIcLGtsLgz5Gi8Qexdu', 'Lam Phong', 'AUTHOR'),
  ('reader1', 'reader1@comic.local', '$2a$12$o5l4khmEgArs.OkS/1sr7OdmAYQNHzfORRAIcLGtsLgz5Gi8Qexdu', 'Minh Anh',  'USER');

INSERT INTO author_profile (user_id, pen_name, bio, approved_at)
SELECT id, 'Lam Phong', 'Tác giả demo, viết cả truyện tranh và truyện chữ.', CURRENT_TIMESTAMP(6)
FROM user_account WHERE username = 'author1';
