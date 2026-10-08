"""python download.py <thư mục tải về> 1,2,3,4,5,6

Tải tranh gốc (gfx_*.png) và bản dịch tiếng Việt (vi/*.svg) của Pepper&Carrot từ các kho lưu trữ chính thức
Deevad/peppercarrot_epNN_translation trên GitHub, cùng phông chữ từ Deevad/peppercarrot_fonts."""
import json
import os
import sys
import urllib.request

OUT = sys.argv[1]
EPISODES = [int(x) for x in sys.argv[2].split(',')]
RAW = 'https://raw.githubusercontent.com/Deevad/{repo}/HEAD/{path}'
FONTS = ['Latin/Lavi.ttf', 'Latin/Lavi_Bold.ttf', 'Latin/Lavi_Italic.ttf', 'Vietnamese/rounded-mplus-1c-medium.ttf',
         'Latin/Fondamento.ttf', 'Latin/AlexBrush.otf', 'Latin/YanoneKaffeesatz-Regular.otf', 'Latin/YanoneKaffeesatz-Bold.otf']


def fetch(repo, path, dest):
    if os.path.exists(dest) and os.path.getsize(dest) > 0:
        return
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    url = RAW.format(repo=repo, path=urllib.request.quote(path))
    with urllib.request.urlopen(url, timeout=120) as response, open(dest, 'wb') as out:
        out.write(response.read())


for font in FONTS:
    fetch('peppercarrot_fonts', font, os.path.join(OUT, 'fonts', os.path.basename(font)))

for n in EPISODES:
    repo = 'peppercarrot_ep%02d_translation' % n
    tree_url = 'https://api.github.com/repos/Deevad/%s/git/trees/HEAD?recursive=1' % repo
    with urllib.request.urlopen(tree_url, timeout=120) as response:
        tree = json.load(response)['tree']
    for item in tree:
        path = item['path']
        if item['type'] != 'blob':
            continue
        if (path.startswith('gfx_') and path.endswith('.png')) or (path.startswith('vi/') and path.endswith('.svg')) \
                or path == 'README.md':
            fetch(repo, path, os.path.join(OUT, 'ep%02d' % n, path))
    print('episode', n, 'ok', flush=True)
