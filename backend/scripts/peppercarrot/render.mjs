// Ghép lớp chữ tiếng Việt (SVG) lên tranh gốc (PNG) của Pepper&Carrot bằng Edge headless rồi xuất JPEG.
// node pc_render.mjs <thư mục nguồn pc> <thư mục ra> <rộng> <chất lượng> <tập,tập,...>
import { spawn } from 'node:child_process';
import { readFileSync, writeFileSync, readdirSync, mkdirSync, mkdtempSync, rmSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { pathToFileURL } from 'node:url';

const [srcDir, outDir, widthArg, qualityArg, episodesArg] = process.argv.slice(2);
const WIDTH = Number(widthArg);
const QUALITY = Number(qualityArg);
const EPISODES = episodesArg.split(',').map(Number);
const PORT = 9333 + Math.floor(Math.random() * 500);
const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const fontsUrl = pathToFileURL(resolve(srcDir, 'fonts')).href;

const FONT_FACES = [
  ['Lavi', 'Lavi.ttf', 'normal', 'normal'],
  ['Lavi', 'Lavi_Bold.ttf', 'bold', 'normal'],
  ['Lavi', 'Lavi_Italic.ttf', 'normal', 'italic'],
  ['Rounded M+ 1c', 'rounded-mplus-1c-medium.ttf', '100 900', 'normal'],
  ['Fondamento', 'Fondamento.ttf', 'normal', 'normal'],
  ['Alex Brush', 'AlexBrush.otf', 'normal', 'normal'],
].map(([family, file, weight, style]) =>
  `@font-face{font-family:'${family}';src:url('${fontsUrl}/${file}');font-weight:${weight};font-style:${style}}`).join('\n');

const edge = spawn(EDGE, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--allow-file-access-from-files',
  `--remote-debugging-port=${PORT}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'edge-pc-'))}`, 'about:blank'],
  { stdio: 'ignore' });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function connect() {
  for (let attempt = 0; attempt < 50; attempt++) {
    try {
      const pages = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
      const page = pages.find((item) => item.type === 'page');
      if (page) return new WebSocket(page.webSocketDebuggerUrl);
    } catch (error) { /* Edge chưa mở cổng */ }
    await sleep(200);
  }
  throw new Error('Không kết nối được tới Edge');
}

const socket = await connect();
await new Promise((r) => socket.addEventListener('open', r));
let nextId = 1;
const pending = new Map();
socket.addEventListener('message', (event) => {
  const message = JSON.parse(event.data);
  if (message.id && pending.has(message.id)) {
    pending.get(message.id)(message.result || message.error);
    pending.delete(message.id);
  }
});
const send = (method, params = {}) => new Promise((r) => {
  const id = nextId++;
  pending.set(id, r);
  socket.send(JSON.stringify({ id, method, params }));
});
await send('Page.enable');

/** Đưa SVG vào trang HTML (để @font-face áp dụng được), ép kích thước theo viewBox. */
function buildHtml(svgText, width, height, cover) {
  let svg = svgText.replace(/<\?xml[^>]*\?>/, '').replace(/<!DOCTYPE[^>]*>/, '');
  const open = svg.match(/<svg\b[^>]*>/s)[0];
  const w = Number(open.match(/\swidth="([\d.]+)/)[1]);
  const h = Number(open.match(/\sheight="([\d.]+)/)[1]);
  let tag = open.replace(/\swidth="[^"]*"/, '').replace(/\sheight="[^"]*"/, '');
  if (!/viewBox=/.test(tag)) tag = tag.replace('<svg', `<svg viewBox="0 0 ${w} ${h}"`);
  tag = tag.replace('<svg', `<svg width="${width}" height="${height}" preserveAspectRatio="${cover ? 'xMidYMid slice' : 'xMidYMid meet'}"`);
  svg = svg.replace(open, tag);
  return { w, h, html: `<!doctype html><meta charset="utf-8"><style>${FONT_FACES}
html,body{margin:0;background:#fff;overflow:hidden}svg{display:block}
.flow{overflow:visible;margin:0}.flow p{margin:0}</style>${svg}<script>${FLOW_SCRIPT}</script>` };
}

// Bộ phân tích HTML hạ chữ thường tên thẻ lạ trong SVG (flowRoot → flowroot).
// Inkscape 0.91 viết chữ thoại bằng <flowRoot> (SVG 1.2, trình duyệt không vẽ). Đổi mỗi flowRoot thành
// <foreignObject> chứa đoạn HTML tự xuống dòng, đặt đúng khung flowRegion và giữ kiểu chữ.
const FLOW_SCRIPT = `
(function () {
  var NS = 'http://www.w3.org/2000/svg', XH = 'http://www.w3.org/1999/xhtml';
  function parse(style) {
    var out = {};
    (style || '').split(';').forEach(function (pair) {
      var i = pair.indexOf(':');
      if (i > 0) out[pair.slice(0, i).trim()] = pair.slice(i + 1).trim();
    });
    return out;
  }
  function apply(el, svgStyle) {
    var map = { 'font-family': 'fontFamily', 'font-size': 'fontSize', 'font-weight': 'fontWeight',
      'font-style': 'fontStyle', 'line-height': 'lineHeight', 'text-align': 'textAlign',
      'letter-spacing': 'letterSpacing', 'word-spacing': 'wordSpacing', 'fill': 'color',
      'fill-opacity': 'opacity', 'font-variant': 'fontVariant' };
    Object.keys(svgStyle).forEach(function (key) {
      var value = svgStyle[key];
      if (!map[key] || value === 'none') return;
      if (key === 'font-family') value = value + ', sans-serif';
      // 125% trong CSS được tính ra px ngay tại phần tử khai báo rồi mới kế thừa; Inkscape hiểu là hệ số theo cỡ chữ
      if (key === 'line-height' && /%$/.test(value)) value = String(parseFloat(value) / 100);
      el.style[map[key]] = value;
    });
    if (!svgStyle['text-align'] && svgStyle['text-anchor'] === 'middle') el.style.textAlign = 'center';
    if (svgStyle['text-anchor'] === 'end' && !svgStyle['text-align']) el.style.textAlign = 'end';
    if (svgStyle.stroke && svgStyle.stroke !== 'none' && svgStyle['stroke-width']) {
      el.style.webkitTextStroke = svgStyle['stroke-width'] + 'px ' + svgStyle.stroke;
    }
  }
  function convertInline(node, target) {
    node.childNodes.forEach(function (child) {
      if (child.nodeType === 3) { target.appendChild(document.createTextNode(child.textContent)); return; }
      if (child.nodeType !== 1) return;
      var span = document.createElementNS(XH, 'span');
      apply(span, parse(child.getAttribute('style')));
      convertInline(child, span);
      target.appendChild(span);
    });
  }
  Array.prototype.slice.call(document.getElementsByTagNameNS(NS, 'flowroot')).forEach(function (flow) {
    var region = flow.getElementsByTagNameNS(NS, 'flowregion')[0];
    var shape = region && region.firstElementChild;
    if (!shape) { flow.remove(); return; }
    // Đo khung bằng một bản sao đặt tạm vào cây được vẽ
    var probe = shape.cloneNode(true);
    probe.removeAttribute('transform');
    flow.parentNode.insertBefore(probe, flow);
    var box = probe.getBBox();
    probe.remove();
    var group = document.createElementNS(NS, 'g');
    var transform = [flow.getAttribute('transform'), shape.getAttribute('transform')].filter(Boolean).join(' ');
    if (transform) group.setAttribute('transform', transform);
    var fo = document.createElementNS(NS, 'foreignObject');
    fo.setAttribute('x', box.x); fo.setAttribute('y', box.y);
    fo.setAttribute('width', box.width); fo.setAttribute('height', box.height + 400);
    fo.setAttribute('overflow', 'visible');
    var div = document.createElementNS(XH, 'div');
    div.setAttribute('class', 'flow');
    div.style.width = box.width + 'px';
    apply(div, parse(flow.getAttribute('style')));
    Array.prototype.slice.call(flow.childNodes).forEach(function (child) {
      if (child.nodeType !== 1 || child.localName.toLowerCase() === 'flowregion') return;
      var p = document.createElementNS(XH, 'p');
      apply(p, parse(child.getAttribute('style')));
      convertInline(child, p);
      if (!p.textContent.trim()) p.textContent = '\\u00a0';
      div.appendChild(p);
    });
    fo.appendChild(div);
    group.appendChild(fo);
    flow.parentNode.replaceChild(group, flow);
  });
  window.__flowDone = true;
})();`;

async function render(svgPath, outPath, coverSize) {
  const text = readFileSync(svgPath, 'utf8');
  const probe = buildHtml(text, 1, 1, false);
  const width = coverSize ? coverSize[0] : WIDTH;
  const height = coverSize ? coverSize[1] : Math.round(WIDTH * probe.h / probe.w);
  const { html } = buildHtml(text, width, height, !!coverSize);
  // File HTML đặt cạnh SVG gốc để đường dẫn ../gfx_*.png trong SVG vẫn đúng
  const htmlPath = svgPath.replace(/\.svg$/, '.render.html');
  writeFileSync(htmlPath, html);
  await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
  await send('Page.navigate', { url: pathToFileURL(resolve(htmlPath)).href });
  await sleep(800);
  await send('Runtime.evaluate', { expression: 'document.fonts.ready.then(() => true)', awaitPromise: true });
  await sleep(1200);
  const shot = await send('Page.captureScreenshot', { format: 'jpeg', quality: QUALITY });
  const data = Buffer.from(shot.data, 'base64');
  writeFileSync(outPath, data);
  return { width, height, size: data.length };
}

const manifest = [];
for (const n of EPISODES) {
  const ep = String(n).padStart(2, '0');
  const viDir = join(srcDir, `ep${ep}`, 'vi');
  const target = join(outDir, `ep${ep}`);
  mkdirSync(target, { recursive: true });
  const files = readdirSync(viDir).filter((f) => f.endsWith('.svg') && !f.startsWith('gfx-only_')).sort();
  // Trang không có lời thoại thì không có bản dịch: dựng một SVG chỉ chứa tranh gốc để không thiếu trang
  for (const gfx of readdirSync(join(srcDir, `ep${ep}`)).filter((f) => /^gfx_.*_E\d+P\d+\.png$/.test(f))) {
    const svgName = gfx.replace(/^gfx_/, '').replace(/\.png$/, '.svg');
    if (files.includes(svgName)) continue;
    const png = readFileSync(join(srcDir, `ep${ep}`, gfx));
    const w = png.readUInt32BE(16);
    const h = png.readUInt32BE(20);
    const name = 'gfx-only_' + svgName;
    writeFileSync(join(viDir, name), `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${w}" height="${h}"><image xlink:href="../${gfx}" x="0" y="0" width="${w}" height="${h}"/></svg>`);
    files.push(name);
  }
  const pageKey = (f) => Number(f.match(/P(\d+)\.svg$/)?.[1] ?? -1);
  files.sort((a, b) => pageKey(a) - pageKey(b));
  const pages = [];
  for (const file of files) {
    const match = file.match(/_E\d+P(\d+)\.svg$/);
    if (!match) continue;
    const pageNo = pages.length + 1;
    const outPath = join(target, `page-${String(pageNo).padStart(2, '0')}.jpg`);
    const info = await render(join(viDir, file), outPath);
    // Trang đệm cuối tập (ảnh trắng cao vài chục điểm ảnh) không có nội dung: bỏ
    if (info.size < 3000) { rmSync(outPath); continue; }
    pages.push({ pageNo, source: file, ...info });
  }
  // Bìa lấy tranh gốc không chữ: tiêu đề tập in sẵn trên bìa sẽ đè lên dải số liệu của thẻ truyện
  const coverPng = `gfx_Pepper-and-Carrot_by-David-Revoy_E${ep}.png`;
  let coverInfo = null;
  if (readdirSync(join(srcDir, `ep${ep}`)).includes(coverPng)) {
    const png = readFileSync(join(srcDir, `ep${ep}`, coverPng));
    const w = png.readUInt32BE(16);
    const h = png.readUInt32BE(20);
    const coverSvg = join(viDir, `gfx-only_cover_E${ep}.svg`);
    writeFileSync(coverSvg, `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${w}" height="${h}"><image xlink:href="../${coverPng}" x="0" y="0" width="${w}" height="${h}"/></svg>`);
    coverInfo = await render(coverSvg, join(target, 'cover.jpg'), [600, 800]);
  }
  manifest.push({ episode: n, pages, cover: coverInfo });
  console.log(`ep${ep}: ${pages.length} trang`);
}
writeFileSync(join(outDir, 'manifest.json'), JSON.stringify(manifest, null, 2));
socket.close();
edge.kill();
process.exit(0);
