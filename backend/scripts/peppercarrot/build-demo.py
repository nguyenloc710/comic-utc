"""python build-demo.py <thư mục ảnh đã dựng> <backend/src/main/resources>

Chép ảnh đã dựng vào demo-media và sinh migration V103 cho truyện Pepper&Carrot."""
import json
import os
import shutil
import sys

RENDERED, RESOURCES = sys.argv[1], sys.argv[2]
MEDIA_DIR = os.path.join(RESOURCES, 'demo-media', 'demo', 'peppercarrot')
SQL_PATH = os.path.join(RESOURCES, 'db', 'demo', 'V103__demo_peppercarrot.sql')

TITLES = ['Thuốc lơ lửng', 'Thuốc cầu vồng', 'Nguyên liệu bí mật', 'Khoảnh khắc thiên tài',
          'Giáng sinh, tập đặc biệt', 'Cuộc thi thần dược']
# Tập mới nhất đăng cách đây 1 ngày, các tập trước cách nhau 5 ngày (phút)
LAST_MINUTES_AGO = 1440
STEP_MINUTES = 7200
CHAPTER_VIEWS = [1840, 1325, 1112, 954, 803, 688]
DAILY_VIEWS = [212, 188, 175, 160, 149, 151, 137, 120, 118, 104, 96, 90, 84, 80]

manifest = json.load(open(os.path.join(RENDERED, 'manifest.json'), encoding='utf-8'))
shutil.rmtree(MEDIA_DIR, ignore_errors=True)
os.makedirs(MEDIA_DIR)
shutil.copy(os.path.join(RENDERED, 'ep01', 'cover.jpg'), os.path.join(MEDIA_DIR, 'cover.jpg'))
CREDITS = ('Pepper&Carrot - David Revoy - https://www.peppercarrot.com',
           'License: Creative Commons Attribution 4.0 (CC BY 4.0) - https://creativecommons.org/licenses/by/4.0/',
           'Vietnamese translation: Binh Pham',
           'Changes: Vietnamese text layer merged into the artwork, images resized to 800px wide, '
           'blank spacer pages removed.')
with open(os.path.join(MEDIA_DIR, 'CREDITS.txt'), 'w', encoding='utf-8', newline='\n') as credits:
    credits.write('\n'.join(CREDITS) + '\n')

DESCRIPTION = (
    'Hạt Tiêu là cô phù thủy nhỏ sống cùng chú mèo Cà Rốt ở rìa khu rừng của thế giới Hereva. Ngày ngày cô '
    'miệt mài pha chế thần dược, và chẳng mẻ thuốc nào diễn ra đúng như dự định.\n\n'
    'Webcomic Pepper&Carrot của họa sĩ David Revoy (www.peppercarrot.com), phát hành theo giấy phép Creative '
    'Commons Attribution 4.0 (CC BY 4.0). Bản dịch tiếng Việt: Binh Pham. Dữ liệu demo đã được chỉnh sửa: '
    'ghép lời thoại vào tranh và thu nhỏ ảnh để hiển thị trên web.')

total_views = sum(CHAPTER_VIEWS)
lines = [
    '-- Truyện tranh demo có ảnh thật: 6 tập đầu của webcomic Pepper&Carrot (David Revoy, www.peppercarrot.com),',
    '-- giấy phép Creative Commons Attribution 4.0 — https://creativecommons.org/licenses/by/4.0/',
    '-- Bản dịch tiếng Việt: Binh Pham (kho Deevad/peppercarrot_epNN_translation trên GitHub).',
    '-- Thay đổi so với bản gốc: ghép lớp chữ tiếng Việt vào tranh, thu nhỏ còn rộng 800px, bỏ trang đệm trống.',
    '-- Ảnh nằm ở resources/demo-media/demo/peppercarrot; cách dựng lại ghi ở backend/scripts/peppercarrot/README.md.',
    '',
    "INSERT INTO user_account (username, email, password_hash, display_name, role) VALUES",
    "  ('peppercarrot', 'peppercarrot@comic.local', '$2a$12$o5l4khmEgArs.OkS/1sr7OdmAYQNHzfORRAIcLGtsLgz5Gi8Qexdu', "
    "'Pepper&Carrot (CC BY 4.0)', 'AUTHOR');",
    "INSERT INTO author_profile (user_id, pen_name, bio, approved_at)",
    "SELECT id, 'David Revoy', 'Tài khoản demo đăng lại webcomic Pepper&Carrot của họa sĩ David Revoy theo giấy phép "
    "CC BY 4.0. Đây không phải tài khoản của tác giả.', NOW(6) - INTERVAL 50000 MINUTE",
    "FROM user_account WHERE username = 'peppercarrot';",
    "SET @u_pc = (SELECT id FROM user_account WHERE username = 'peppercarrot');",
    '',
    "INSERT INTO story (slug, title, alt_title, description, cover_path, type, status, visibility, hidden_reason, "
    "author_id, chapter_count, view_count, follow_count, rating_sum, rating_count, comment_count, published_at, "
    "last_chapter_at, created_at) VALUES",
    "  ('hat-tieu-va-ca-rot', 'Hạt Tiêu & Cà Rốt', 'Pepper&Carrot', '%s', 'demo/peppercarrot/cover.jpg', 'COMIC', "
    "'ONGOING', 'PUBLISHED', NULL, @u_pc, %d, %d, 0, 0, 0, 0, NOW(6) - INTERVAL %d MINUTE, NOW(6) - INTERVAL %d MINUTE, "
    "NOW(6) - INTERVAL %d MINUTE);" % (
        DESCRIPTION.replace("'", "''").replace('\n', '\\n'), len(manifest), total_views,
        LAST_MINUTES_AGO + STEP_MINUTES * (len(manifest) - 1), LAST_MINUTES_AGO,
        LAST_MINUTES_AGO + STEP_MINUTES * len(manifest)),
    'SET @s_pc = LAST_INSERT_ID();',
    "INSERT INTO story_genre (story_id, genre_id) SELECT @s_pc, id FROM genre WHERE slug IN "
    "('phieu-luu', 'huyen-huyen', 'hai-huoc');",
    '',
]

for index, episode in enumerate(manifest):
    n = episode['episode']
    minutes_ago = LAST_MINUTES_AGO + STEP_MINUTES * (len(manifest) - 1 - index)
    lines.append("INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, "
                 "word_count, view_count) VALUES (@s_pc, %d, '%s', 'PUBLISHED', NULL, NOW(6) - INTERVAL %d MINUTE, "
                 "%d, 0, %d);" % (n, TITLES[index], minutes_ago, len(episode['pages']), CHAPTER_VIEWS[index]))
    lines.append('SET @c_pc = LAST_INSERT_ID();')
    rows = []
    target = os.path.join(MEDIA_DIR, 'ep%02d' % n)
    os.makedirs(target)
    for page in episode['pages']:
        name = 'page-%02d.jpg' % page['pageNo']
        shutil.copy(os.path.join(RENDERED, 'ep%02d' % n, name), os.path.join(target, name))
        rows.append("  (@c_pc, %d, 'demo/peppercarrot/ep%02d/%s', %d, %d, %d)" % (
            page['pageNo'], n, name, page['width'], page['height'], page['size']))
    lines.append('INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES')
    lines.append(',\n'.join(rows) + ';')
    lines.append('')

lines.append('-- Lượt xem theo ngày để truyện có mặt trong bảng xếp hạng tuần / tháng')
lines.append('INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES')
lines.append(',\n'.join("  (@s_pc, DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00')) - INTERVAL %d DAY, %d)" % (day, views)
                        for day, views in enumerate(DAILY_VIEWS)) + ';')

with open(SQL_PATH, 'w', encoding='utf-8', newline='\n') as out:
    out.write('\n'.join(lines) + '\n')
print('pages', sum(len(e['pages']) for e in manifest), 'sql', SQL_PATH)
