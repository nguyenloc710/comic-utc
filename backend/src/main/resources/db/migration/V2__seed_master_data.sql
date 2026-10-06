-- Dữ liệu chủ: thể loại, tham số vận hành, tài khoản quản trị mặc định.

-- Mô tả thể loại không chỉ để hiển thị: chatbot dựa vào đó để ánh xạ lời người dùng sang slug
-- (ví dụ "nữ chính mạnh mẽ" → nu-cuong), nên mỗi mô tả phải nêu được dấu hiệu nhận biết.
INSERT INTO genre (name, slug, description, sort_order) VALUES
  ('Hành động',            'hanh-dong',            'Nhiều cảnh chiến đấu, rượt đuổi, nhịp truyện nhanh và kịch tính.', 1),
  ('Phiêu lưu',            'phieu-luu',            'Nhân vật lên đường khám phá vùng đất mới, vượt thử thách trên hành trình dài.', 2),
  ('Tu tiên',              'tu-tien',              'Nhân vật tu luyện để trường sinh thành tiên: luyện khí, độ kiếp, tông môn, linh căn.', 3),
  ('Huyền huyễn',          'huyen-huyen',          'Thế giới giả tưởng phương Đông với phép thuật, thần thú và hệ thống sức mạnh riêng.', 4),
  ('Tiên hiệp',            'tien-hiep',            'Thế giới tiên nhân, pháp bảo, môn phái tu chân đậm màu sắc Đạo giáo.', 5),
  ('Kiếm hiệp',            'kiem-hiep',            'Giang hồ võ lâm, hiệp khách, môn phái và ân oán nghĩa tình.', 6),
  ('Võ thuật',             'vo-thuat',             'Xoay quanh việc luyện võ, thi đấu đối kháng và tinh thần thượng võ.', 7),
  ('Xuyên không',          'xuyen-khong',          'Nhân vật vượt thời gian hoặc sang thế giới khác và sống một cuộc đời mới ở đó.', 8),
  ('Trọng sinh',           'trong-sinh',           'Nhân vật chết đi rồi sống lại, quay về quá khứ để làm lại cuộc đời.', 9),
  ('Nữ cường',             'nu-cuong',             'Nữ chính mạnh mẽ, độc lập, tài giỏi, tự quyết định số phận của mình.', 10),
  ('Ngôn tình',            'ngon-tinh',            'Chuyện tình cảm nam nữ lãng mạn là trục chính của câu chuyện.', 11),
  ('Đam mỹ',               'dam-my',               'Chuyện tình cảm giữa các nhân vật nam.', 12),
  ('Bách hợp',             'bach-hop',             'Chuyện tình cảm giữa các nhân vật nữ.', 13),
  ('Cổ đại',               'co-dai',               'Bối cảnh thời xưa: cung đình, quan trường, hậu cung, chiến trận.', 14),
  ('Đô thị',               'do-thi',               'Bối cảnh thành phố hiện đại: công sở, thương trường, cuộc sống thường nhật.', 15),
  ('Học đường',            'hoc-duong',            'Bối cảnh trường lớp, tình bạn và những rung động tuổi học trò.', 16),
  ('Hài hước',             'hai-huoc',             'Tình huống gây cười, giọng kể dí dỏm, đọc để giải trí nhẹ nhàng.', 17),
  ('Kinh dị',              'kinh-di',              'Ma quỷ, hiện tượng siêu nhiên, không khí rùng rợn gây sợ hãi.', 18),
  ('Trinh thám',           'trinh-tham',           'Phá án, suy luận, truy tìm thủ phạm qua từng manh mối.', 19),
  ('Bí ẩn',                'bi-an',                'Câu chuyện xoay quanh một bí mật lớn được hé lộ dần.', 20),
  ('Khoa học viễn tưởng',  'khoa-hoc-vien-tuong',  'Công nghệ tương lai, du hành vũ trụ, người máy, trí tuệ nhân tạo.', 21),
  ('Mạt thế',              'mat-the',              'Thế giới sau thảm họa tận thế: xác sống, dị năng, con người đấu tranh sinh tồn.', 22),
  ('Hệ thống',             'he-thong',             'Nhân vật được một hệ thống giao nhiệm vụ và thưởng điểm, kỹ năng để thăng cấp.', 23),
  ('Dị giới',              'di-gioi',              'Bối cảnh thế giới khác kiểu phương Tây: kiếm và phép thuật, ma vương, mạo hiểm giả.', 24),
  ('Game',                 'game',                 'Câu chuyện diễn ra trong trò chơi trực tuyến hoặc thế giới vận hành theo luật game.', 25),
  ('Lịch sử',              'lich-su',              'Dựa trên hoặc lấy cảm hứng từ sự kiện, nhân vật lịch sử có thật.', 26),
  ('Quân sự',              'quan-su',              'Chiến tranh, binh pháp, đời sống người lính.', 27),
  ('Thể thao',             'the-thao',             'Xoay quanh một môn thể thao, tinh thần đồng đội và thi đấu.', 28),
  ('Đời thường',           'doi-thuong',           'Lát cắt cuộc sống hằng ngày, nhẹ nhàng, ít xung đột, chữa lành.', 29),
  ('Gia đình',             'gia-dinh',             'Tình cảm và mâu thuẫn giữa các thành viên trong gia đình.', 30),
  ('Điền văn',             'dien-van',             'Làm ruộng, kinh doanh, xây dựng cuộc sống no đủ ở nông thôn một cách thong thả.', 31),
  ('Ẩm thực',              'am-thuc',              'Nấu nướng, món ăn và đam mê ẩm thực là trung tâm câu chuyện.', 32);

INSERT INTO setting (setting_key, setting_value, value_type, group_name, description) VALUES
  ('security.password.min_length',       '8',    'INTEGER', 'SECURITY', 'Độ dài mật khẩu tối thiểu'),
  ('security.login.max_failed_attempts', '5',    'INTEGER', 'SECURITY', 'Số lần sai mật khẩu trước khi khóa tạm'),
  ('security.login.lock_minutes',        '15',   'INTEGER', 'SECURITY', 'Số phút khóa tạm sau khi sai quá số lần cho phép'),
  ('author.request.cooldown.days',       '7',    'INTEGER', 'AUTHOR',   'Số ngày chờ trước khi gửi lại yêu cầu làm tác giả bị từ chối'),
  ('upload.image.max_size_mb',           '5',    'INTEGER', 'UPLOAD',   'Dung lượng tối đa mỗi ảnh (MB)'),
  ('upload.chapter.max_pages',           '150',  'INTEGER', 'UPLOAD',   'Số ảnh tối đa mỗi chương truyện tranh'),
  ('comment.cooldown.seconds',           '15',   'INTEGER', 'COMMENT',  'Số giây tối thiểu giữa hai bình luận của một người'),
  ('view.dedupe.minutes',                '30',   'INTEGER', 'STATS',    'Trong khoảng này, một phiên mở lại cùng chương chỉ tính một lượt xem'),
  ('ranking.min_rating_count',           '5',    'INTEGER', 'STATS',    'Số lượt đánh giá tối thiểu để truyện vào bảng xếp hạng theo điểm'),
  ('chat.enabled',                       'true', 'BOOLEAN', 'CHAT',     'Bật/tắt chatbot gợi ý truyện'),
  ('chat.daily_limit_per_user',          '50',   'INTEGER', 'CHAT',     'Số tin nhắn mỗi người được gửi cho chatbot mỗi ngày'),
  ('chat.max_message_length',            '500',  'INTEGER', 'CHAT',     'Độ dài tối đa một tin nhắn gửi chatbot'),
  ('chat.history_window',                '10',   'INTEGER', 'CHAT',     'Số tin gần nhất gửi kèm cho mô hình để hỏi nối tiếp'),
  ('chat.max_recommendations',           '6',    'INTEGER', 'CHAT',     'Số thẻ truyện tối đa trong một câu trả lời'),
  ('chat.retention.days',                '90',   'INTEGER', 'CHAT',     'Số ngày giữ hội thoại trước khi xóa');

-- Mật khẩu mặc định: Admin@123 (BCrypt cost 12). PHẢI đổi ngay sau khi triển khai thật.
INSERT INTO user_account (username, email, password_hash, display_name, role) VALUES
  ('admin', 'admin@comic.local', '$2a$12$f1A5eXbqAnVjCnznycyORuE6uanheA9./UECHzCWvU1ZJsJkJt.7O', 'Quản trị viên', 'ADMIN');
