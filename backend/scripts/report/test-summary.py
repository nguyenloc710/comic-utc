"""python test-summary.py <backend/target> <tệp .md đầu ra>

Gom kết quả của lần `./mvnw verify` gần nhất (Surefire + JaCoCo) thành bảng Markdown cho chương 4 của báo cáo.
Chạy lại sau mỗi lần verify để số liệu trong báo cáo khớp với mã nguồn."""
import csv
import glob
import os
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import datetime

TARGET, OUTPUT = sys.argv[1], sys.argv[2]
ROOT_PACKAGE = 'vn.edu.utc.comic.'

# Tên hiển thị của từng module theo package gốc
MODULES = {
    'auth': 'Tài khoản, đăng nhập, hồ sơ', 'author': 'Đăng ký tác giả', 'chapter': 'Chương, đăng/hẹn giờ, trang đọc',
    'chatbot': 'Chatbot AI', 'common': 'Hạ tầng dùng chung (bảo mật, lưu ảnh, lỗi, tiện ích)', 'genre': 'Thể loại',
    'interaction': 'Theo dõi, đánh giá, bình luận', 'notification': 'Thông báo', 'report': 'Báo cáo vi phạm',
    'stats': 'Thống kê, xếp hạng', 'story': 'Truyện, tìm kiếm, kiểm duyệt', 'system': 'Tham số, nhật ký',
    'user': 'Quản lý người dùng', '': 'Toàn hệ thống (lược đồ, phân quyền)',
}


def module_of(class_name):
    rest = class_name[len(ROOT_PACKAGE):] if class_name.startswith(ROOT_PACKAGE) else class_name
    return rest.split('.')[0] if '.' in rest else ''


def read_suites():
    suites = []
    for path in sorted(glob.glob(os.path.join(TARGET, 'surefire-reports', 'TEST-*.xml'))):
        root = ET.parse(path).getroot()
        cases = [(case.get('name'), 'skip' if case.find('skipped') is not None
                  else 'fail' if case.find('failure') is not None or case.find('error') is not None else 'pass')
                 for case in root.iter('testcase')]
        suites.append({'name': root.get('name'), 'time': float(root.get('time', 0)), 'cases': cases})
    return suites


def read_coverage():
    lines = defaultdict(lambda: [0, 0])
    branches = defaultdict(lambda: [0, 0])
    with open(os.path.join(TARGET, 'site', 'jacoco', 'jacoco.csv'), encoding='utf-8') as source:
        for row in csv.DictReader(source):
            package = row['PACKAGE'][len(ROOT_PACKAGE):]
            lines[package][0] += int(row['LINE_COVERED'])
            lines[package][1] += int(row['LINE_COVERED']) + int(row['LINE_MISSED'])
            branches[package][0] += int(row['BRANCH_COVERED'])
            branches[package][1] += int(row['BRANCH_COVERED']) + int(row['BRANCH_MISSED'])
    return lines, branches


def percent(pair):
    return '–' if pair[1] == 0 else f'{100 * pair[0] / pair[1]:.1f}%'


def count(suites, status):
    return sum(1 for suite in suites for _, result in suite['cases'] if result == status)


suites = read_suites()
lines, branches = read_coverage()
by_module = defaultdict(list)
for suite in suites:
    by_module[module_of(suite['name'])].append(suite)

out = [f'# Kết quả kiểm thử tự động',
       '',
       f'Sinh bởi `backend/scripts/report/test-summary.py` từ lần `./mvnw verify` lúc '
       f'{datetime.fromtimestamp(os.path.getmtime(os.path.join(TARGET, "site", "jacoco", "jacoco.csv"))):%d/%m/%Y %H:%M}. '
       'Test tích hợp chạy trên MySQL 8.4 thật (Testcontainers), không gọi mô hình ngôn ngữ thật.',
       '',
       '## Tổng hợp theo module',
       '',
       '| Module | Lớp test | Ca kiểm thử | Đạt | Lỗi | Bỏ qua | Thời gian (s) |',
       '|---|---:|---:|---:|---:|---:|---:|']
for module in sorted(by_module, key=lambda key: MODULES.get(key, key)):
    group = by_module[module]
    out.append(f'| {MODULES.get(module, module)} | {len(group)} | {sum(len(s["cases"]) for s in group)} | '
               f'{count(group, "pass")} | {count(group, "fail")} | {count(group, "skip")} | '
               f'{sum(s["time"] for s in group):.1f} |')
out.append(f'| **Tổng** | **{len(suites)}** | **{sum(len(s["cases"]) for s in suites)}** | **{count(suites, "pass")}** | '
           f'**{count(suites, "fail")}** | **{count(suites, "skip")}** | **{sum(s["time"] for s in suites):.1f}** |')

total_lines = [sum(v[0] for v in lines.values()), sum(v[1] for v in lines.values())]
total_branches = [sum(v[0] for v in branches.values()), sum(v[1] for v in branches.values())]
out += ['', '## Độ phủ mã (JaCoCo)', '',
        f'Toàn bộ: **{percent(total_lines)}** số dòng, **{percent(total_branches)}** số nhánh. '
        'Ngưỡng bắt buộc trong `pom.xml`: mỗi package `*.service` phủ tối thiểu 60% số dòng.',
        '', '| Package | Dòng | Nhánh |', '|---|---:|---:|']
for package in sorted(lines):
    if package.endswith('.service'):
        out.append(f'| `{package}` | {percent(lines[package])} | {percent(branches[package])} |')

out += ['', '## Danh sách ca kiểm thử', '']
for module in sorted(by_module, key=lambda key: MODULES.get(key, key)):
    out += [f'### {MODULES.get(module, module)}', '']
    for suite in by_module[module]:
        out.append(f'**{suite["name"].rsplit(".", 1)[-1]}**')
        out.append('')
        for name, result in suite['cases']:
            mark = {'pass': '✔', 'fail': '✘', 'skip': '○'}[result]
            out.append(f'- {mark} `{name}`')
        out.append('')

os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
with open(OUTPUT, 'w', encoding='utf-8', newline='\n') as target:
    target.write('\n'.join(out) + '\n')
print('suites', len(suites), 'cases', sum(len(s['cases']) for s in suites), '->', OUTPUT)
