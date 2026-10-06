// Sinh dữ liệu demo cho môi trường dev: truyện, chương, ảnh giữ chỗ, lượt xem, theo dõi, đánh giá, bình luận.
//
//   cd backend && node scripts/generate-demo-data.mjs
//
// Kết quả (đều được commit, máy chạy demo không cần Node):
//   src/main/resources/db/demo/V101__demo_content.sql
//   src/main/resources/demo-media/demo/covers/*.svg   ảnh bìa
//   src/main/resources/demo-media/demo/pages/*.svg    trang truyện tranh dùng chung
//
// Bộ sinh số ngẫu nhiên có seed cố định nên chạy lại cho đúng kết quả cũ. Mọi nội dung đều tự sinh từ các
// mảnh câu viết riêng cho dự án; không lấy truyện hay ảnh từ nguồn nào khác. Mốc thời gian trong SQL là tương
// đối so với lúc nạp (NOW() - INTERVAL ...), nên nạp ngày nào cũng có truyện "vừa cập nhật".

import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const RESOURCES = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'main', 'resources');
const SQL_FILE = join(RESOURCES, 'db', 'demo', 'V101__demo_content.sql');
const MEDIA_DIR = join(RESOURCES, 'demo-media', 'demo');

const SEED = 20261006;
const STORY_COUNT = 64;
const PAGE_POOL_SIZE = 12;
const PAGE_WIDTH = 800;
const PAGE_HEIGHT = 1200;
const VIEW_HISTORY_DAYS = 60;
const MINUTES_PER_DAY = 1440;
const DEMO_PASSWORD_HASH = '$2a$12$o5l4khmEgArs.OkS/1sr7OdmAYQNHzfORRAIcLGtsLgz5Gi8Qexdu'; // Demo@123

// ---------- Số ngẫu nhiên có seed ----------
let state = SEED;
function random() {
  state |= 0;
  state = (state + 0x6d2b79f5) | 0;
  let t = Math.imul(state ^ (state >>> 15), 1 | state);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
}
const between = (min, max) => min + Math.floor(random() * (max - min + 1));
const pick = (items) => items[Math.floor(random() * items.length)];
function sample(items, count) {
  const pool = [...items];
  const chosen = [];
  while (chosen.length < count && pool.length > 0) {
    chosen.push(pool.splice(Math.floor(random() * pool.length), 1)[0]);
  }
  return chosen;
}

// ---------- Tiện ích ----------
/** Cùng quy tắc với SlugUtils.toSlug ở back-end. */
function slugify(text) {
  return text.toLowerCase().replaceAll('đ', 'd').normalize('NFD').replace(/\p{M}+/gu, '')
    .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}
const sql = (value) => (value === null ? 'NULL' : `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")}'`);
const minutesAgo = (minutes) => `NOW(6) - INTERVAL ${Math.max(0, Math.round(minutes))} MINUTE`;
const xml = (text) => text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');

// ---------- Tài khoản ----------
const AUTHORS = [
  { username: 'author1', penName: 'Lam Phong', existing: true },
  { username: 'author2', displayName: 'Hạ Vũ', penName: 'Hạ Vũ', bio: 'Viết truyện chữ cổ đại và tu tiên.' },
  { username: 'author3', displayName: 'Trúc Lâm', penName: 'Trúc Lâm Cư Sĩ', bio: 'Vẽ truyện tranh đời thường, học đường.' },
];
const READER_NAMES = ['Minh Anh', 'Quang Huy', 'Thu Trang', 'Đức Anh', 'Ngọc Mai', 'Hoàng Nam', 'Khánh Linh', 'Tuấn Kiệt',
  'Bảo Ngọc', 'Gia Hân', 'Thành Đạt', 'Phương Thảo', 'Hải Đăng', 'Yến Nhi', 'Trung Kiên'];
const READERS = READER_NAMES.map((displayName, index) => ({
  username: `reader${index + 1}`, displayName, existing: index === 0,
}));

// ---------- Chủ đề truyện: thể loại, mảnh ghép tên, mẫu mô tả ----------
const HEROES = ['Lâm Phong', 'Trần Vũ', 'Hạ Linh', 'Mộc Thanh', 'Tô Nguyệt', 'Diệp An', 'Khương Nhiên', 'Bạch Dương'];
const THEMES = [
  { genres: ['tu-tien', 'huyen-huyen', 'hanh-dong'], types: ['NOVEL', 'NOVEL', 'COMIC'], color: ['#4c1d95', '#7c3aed'],
    first: ['Vạn Cổ', 'Cửu Thiên', 'Thanh Vân', 'Huyền Thiên', 'Vô Cực', 'Thái Hư', 'Tinh Hà'],
    second: ['Kiếm Tôn', 'Đan Đế', 'Tiên Lộ', 'Đạo Quân', 'Thần Vương', 'Linh Chủ'],
    descriptions: [
      '{hero} vốn chỉ là đệ tử ngoại môn có linh căn tầm thường, bị cả tông môn xem nhẹ. Một lần rơi xuống vực sâu, cậu nhặt được mảnh ngọc cổ chứa truyền thừa của một vị tiên nhân đã ngã xuống. Từ đó con đường tu tiên mở ra: luyện khí, trúc cơ, độ kiếp, từng bước leo lên đỉnh cao của giới tu chân.',
      'Thiên địa đại biến, linh khí khôi phục. {hero} mang theo ký ức mơ hồ về một kiếp trước bước vào con đường tu luyện, đối đầu với các tông môn lớn và những bí mật bị chôn vùi từ thời thượng cổ.' ] },
  { genres: ['ngon-tinh', 'do-thi', 'doi-thuong'], types: ['NOVEL', 'COMIC'], color: ['#be185d', '#f472b6'],
    first: ['Gió Mùa', 'Nắng Cuối', 'Mưa Đầu', 'Hẹn Ước', 'Lời Hứa', 'Ký Ức'],
    second: ['Thu Hà Nội', 'Bên Sông', 'Tháng Tư', 'Ngày Ấy', 'Mùa Hoa Sữa', 'Phố Cũ'],
    descriptions: [
      'Chuyện tình nhẹ nhàng giữa {hero}, cô biên tập viên trẻ vừa chuyển về thành phố, và người hàng xóm ít nói ở căn hộ đối diện. Hai con người từng tổn thương học cách tin tưởng và yêu thương thêm một lần nữa.',
      'Mười năm sau ngày tốt nghiệp, {hero} gặp lại mối tình đầu trong một buổi họp lớp. Những hiểu lầm cũ dần được gỡ bỏ giữa nhịp sống hối hả của thành phố.' ] },
  { genres: ['hoc-duong', 'hai-huoc', 'doi-thuong'], types: ['COMIC', 'COMIC', 'NOVEL'], color: ['#b45309', '#fbbf24'],
    first: ['Lớp Trưởng', 'Câu Lạc Bộ', 'Bàn Cuối', 'Hội Bạn', 'Giờ Ra Chơi'],
    second: ['Bá Đạo', 'Siêu Quậy', 'Lầy Lội', 'Của Tôi', 'Không Ngủ'],
    descriptions: [
      'Những ngày tháng học trò dở khóc dở cười của {hero} và hội bạn thân ở lớp 11A5: trốn tiết, thi cử, văn nghệ và cả những rung động đầu đời. Truyện hài hước, đọc để giải trí sau giờ học.',
      '{hero} chuyển trường giữa học kỳ và bị xếp ngồi cạnh lớp trưởng khó tính nhất khối. Một năm học ồn ào và đáng nhớ bắt đầu từ đó.' ] },
  { genres: ['kinh-di', 'bi-an'], types: ['NOVEL', 'COMIC'], color: ['#111827', '#4b5563'],
    first: ['Căn Phòng', 'Ngôi Làng', 'Tiếng Gõ', 'Bóng Người', 'Chuyến Tàu'],
    second: ['Số 13', 'Không Tên', 'Lúc Nửa Đêm', 'Sau Màn Sương', 'Cuối Hành Lang'],
    descriptions: [
      '{hero} thuê được căn phòng giá rẻ bất ngờ ở một khu tập thể cũ. Đêm nào cũng có tiếng gõ cửa đúng ba giờ sáng, và những người hàng xóm dường như đều đang che giấu một bí mật rùng rợn.',
      'Một ngôi làng trên núi không có trong bản đồ, nơi dân làng không bao giờ ra khỏi nhà sau khi mặt trời lặn. {hero} đến đó tìm người chị mất tích và dần hiểu vì sao.' ] },
  { genres: ['trinh-tham', 'bi-an', 'do-thi'], types: ['NOVEL', 'COMIC'], color: ['#1e3a8a', '#3b82f6'],
    first: ['Hồ Sơ', 'Vụ Án', 'Thám Tử', 'Dấu Vết'],
    second: ['Mật Số 7', 'Phố Hàng Bạc', 'Mùa Mưa', 'Không Lời Giải', 'Cuối Cùng'],
    descriptions: [
      'Thám tử tư {hero} nhận một vụ tìm người tưởng chừng đơn giản, rồi phát hiện nó dính tới chuỗi án mạng chưa có lời giải suốt mười năm. Mỗi chương là một manh mối, mỗi manh mối lật ngược suy luận trước đó.',
      'Cựu điều tra viên {hero} bị kéo trở lại nghề khi một lá thư nặc danh mô tả chính xác vụ án đầu tiên mà anh thất bại.' ] },
  { genres: ['nu-cuong', 'xuyen-khong', 'co-dai', 'trong-sinh'], types: ['NOVEL', 'NOVEL', 'COMIC'], color: ['#9f1239', '#fb7185'],
    first: ['Nữ Tướng', 'Đích Nữ', 'Quận Chúa', 'Thần Y', 'Nữ Đế'],
    second: ['Trở Về', 'Khuynh Thành', 'Phá Thiên', 'Trọng Sinh', 'Bất Bại'],
    descriptions: [
      'Kiếp trước {hero} bị hãm hại đến tan cửa nát nhà. Sống lại năm mười sáu tuổi, nàng không còn là tiểu thư yếu đuối: nữ chính mạnh mẽ, thông minh, tự tay giành lại tất cả và che chở những người mình yêu thương.',
      'Nữ bác sĩ quân y {hero} xuyên không về thời cổ đại, trở thành đích nữ bị ghẻ lạnh của phủ tướng quân. Với y thuật và bản lĩnh của mình, nàng từng bước thay đổi số phận.' ] },
  { genres: ['khoa-hoc-vien-tuong', 'mat-the', 'hanh-dong'], types: ['COMIC', 'NOVEL'], color: ['#0f766e', '#2dd4bf'],
    first: ['Trạm Không Gian', 'Kỷ Nguyên', 'Thành Phố', 'Tín Hiệu'],
    second: ['Tro Tàn', 'Số Không', 'Ngầm', 'Từ Sao Hỏa', 'Sau Tận Thế'],
    descriptions: [
      'Năm 2147, một tín hiệu lạ từ rìa hệ Mặt Trời làm tê liệt toàn bộ mạng lưới vệ tinh. Kỹ sư {hero} cùng phi hành đoàn của trạm không gian cuối cùng phải tìm ra nguồn phát trước khi quá muộn.',
      'Sau thảm họa, loài người sống trong các thành phố ngầm. {hero} là thợ sửa máy lọc khí, vô tình tìm được bản đồ dẫn lên mặt đất và sự thật về ngày tận thế.' ] },
  { genres: ['the-thao', 'hoc-duong', 'doi-thuong'], types: ['COMIC'], color: ['#166534', '#4ade80'],
    first: ['Cú Sút', 'Đường Chạy', 'Sân Bóng', 'Tay Vợt'],
    second: ['Quyết Định', 'Mùa Hè', 'Tuổi 17', 'Làng Tôi'],
    descriptions: [
      'Đội bóng trường huyện chưa từng thắng nổi một trận ở giải tỉnh. {hero}, cậu học sinh mới chuyển về với đôi chân trái kỳ lạ, cùng đồng đội viết nên một mùa hè không thể quên.',
      '{hero} bỏ điền kinh sau một chấn thương. Người huấn luyện viên già và đường chạy đất đỏ của trường làng kéo cậu trở lại.' ] },
  { genres: ['am-thuc', 'dien-van', 'doi-thuong', 'gia-dinh'], types: ['COMIC', 'NOVEL'], color: ['#9a3412', '#fb923c'],
    first: ['Quán Nhỏ', 'Bếp Lửa', 'Mảnh Vườn', 'Tiệm Bánh'],
    second: ['Cuối Ngõ', 'Nhà Bà', 'Mùa Gặt', 'Ven Đồi'],
    descriptions: [
      '{hero} nghỉ việc ở thành phố, về quê tiếp quản quán ăn nhỏ của bà. Mỗi chương là một món ăn và câu chuyện của người khách ghé quán. Truyện chữa lành, nhẹ nhàng, đọc xong thấy đói bụng.',
      'Một mảnh vườn bỏ hoang, một căn bếp củi và {hero} bắt đầu lại từ con số không: trồng rau, nuôi gà, mở tiệm bánh đầu tiên của làng.' ] },
  { genres: ['he-thong', 'game', 'di-gioi', 'hanh-dong'], types: ['COMIC', 'NOVEL'], color: ['#3730a3', '#818cf8'],
    first: ['Hệ Thống', 'Người Chơi', 'Anh Hùng', 'Thợ Săn'],
    second: ['Cấp SSS', 'Số Một', 'Dị Giới', 'Hầm Ngục', 'Tối Thượng'],
    descriptions: [
      'Tỉnh dậy trong một thế giới vận hành theo luật trò chơi, {hero} chỉ có một kỹ năng bị coi là vô dụng. Nhưng hệ thống của cậu có một dòng mô tả ẩn mà không người chơi nào khác nhìn thấy.',
      'Cổng hầm ngục xuất hiện khắp thế giới. {hero}, thợ săn hạng thấp nhất, nhận được hệ thống cho phép thăng cấp không giới hạn sau một lần suýt chết.' ] },
];
const HIDDEN_REASON = 'Nội dung chương 3 sao chép từ tác phẩm khác. Vui lòng chỉnh sửa rồi liên hệ quản trị viên.';

const CHAPTER_TITLES = ['Khởi đầu', 'Cuộc gặp bất ngờ', 'Bí mật đầu tiên', 'Lựa chọn', 'Đêm dài', 'Người lạ', 'Thử thách',
  'Lời hứa', 'Dấu vết', 'Bước ngoặt', 'Trở về', 'Sự thật', 'Cơn mưa', 'Đối mặt', 'Hy vọng', 'Ranh giới', 'Kế hoạch',
  'Rạn nứt', 'Bình minh', 'Lối rẽ', 'Món quà', 'Kẻ theo dõi', 'Giao ước', 'Tạm biệt', 'Hồi âm', 'Trước giờ G',
  'Mất dấu', 'Tái ngộ', 'Canh bạc', 'Hồi kết của một ngày'];
const SENTENCES = [
  '{hero} đứng lặng một lúc lâu trước khi quyết định bước tiếp.',
  'Gió thổi qua hành lang mang theo mùi mưa sắp tới.',
  'Không ai nói với ai câu nào, nhưng tất cả đều hiểu chuyện gì vừa xảy ra.',
  'Có những điều chỉ khi mất đi rồi người ta mới biết nó từng quan trọng đến thế.',
  'Ánh đèn cuối phố chập chờn rồi tắt hẳn.',
  '{hero} siết chặt tay, cố giữ cho giọng mình không run.',
  'Câu trả lời nằm ngay trước mắt, chỉ là chưa ai chịu nhìn.',
  'Tiếng bước chân vang lên phía sau, đều đặn và không vội vã.',
  'Ngày hôm đó trời rất trong, đến mức thấy rõ từng ngọn núi ở phía xa.',
  'Mọi kế hoạch đều đẹp cho tới khi chạm vào thực tế.',
  '{hero} bật cười, lần đầu tiên sau nhiều ngày.',
  'Lá thư chỉ có đúng một dòng, viết vội bằng mực xanh.',
  'Người ta bảo thời gian chữa lành mọi thứ, nhưng không ai nói sẽ mất bao lâu.',
  'Cánh cửa mở ra, bên trong tối om.',
  'Đó là lần đầu tiên {hero} hiểu thế nào là sợ hãi thật sự.',
  'Bên kia sông, những ngọn đèn lần lượt sáng lên.',
  'Chuyện tưởng đã kết thúc ở đó, hóa ra mới chỉ là bắt đầu.',
  'Ông lão không ngẩng đầu lên, chỉ khẽ gật một cái.',
  '{hero} nhớ lại lời dặn năm xưa và chợt hiểu ra tất cả.',
  'Cả căn phòng im lặng đến mức nghe được tiếng kim đồng hồ.',
  'Con đường phía trước chia làm hai ngả, ngả nào cũng mờ sương.',
  'Không có phép màu nào cả, chỉ có những người không chịu bỏ cuộc.',
  'Tin nhắn hiện lên rồi biến mất trước khi kịp đọc hết.',
  '{hero} hít một hơi thật sâu rồi đẩy cửa bước vào.',
];
const COMMENTS = ['Truyện hay quá, hóng chương mới!', 'Nét vẽ đẹp, cốt truyện cuốn.', 'Đọc một mạch tới chương mới nhất luôn.',
  'Tác giả ra chương đều ghê, cảm ơn tác giả.', 'Nhân vật chính có chiều sâu, thích cách xây dựng tính cách.',
  'Đoạn cuối chương này bất ngờ thật sự.', 'Mong truyện dài dài một chút.', 'Ai đọc tới đây rồi cho xin cảm nhận với.',
  'Phần đầu hơi chậm nhưng càng về sau càng hay.', 'Lần đầu đọc thể loại này mà thấy cuốn.',
  'Hóng phần tiếp theo quá.', 'Chi tiết ở chương 2 hóa ra là để dành cho đoạn này, hay!',
  'Truyện nhẹ nhàng, đọc buổi tối rất hợp.', 'Cười xỉu với nhân vật phụ.', 'Xin phép đề cử truyện này cho mọi người.'];
const REPLIES = ['Chuẩn luôn bạn ơi.', 'Mình cũng nghĩ vậy.', 'Đọc tiếp đi, sau còn hay hơn.', 'Cảm ơn bạn đã ủng hộ!'];

// ---------- Dựng dữ liệu trong bộ nhớ ----------
function buildNovelContent(hero) {
  const paragraphs = [];
  let wordCount = 0;
  for (let p = 0, total = between(6, 10); p < total; p++) {
    const text = sample(SENTENCES, between(3, 4)).join(' ').replaceAll('{hero}', hero);
    wordCount += text.split(/\s+/).length;
    paragraphs.push(`<p>${text}</p>`);
  }
  return { html: paragraphs.join(''), wordCount };
}

function buildStories() {
  const usedTitles = new Set();
  const stories = [];
  for (let index = 0; index < STORY_COUNT; index++) {
    const theme = THEMES[index % THEMES.length];
    let title;
    do { title = `${pick(theme.first)} ${pick(theme.second)}`; } while (usedTitles.has(title));
    usedTitles.add(title);
    const hero = pick(HEROES);
    const type = theme.types[Math.floor(index / THEMES.length) % theme.types.length];
    const statusRoll = random();
    const status = statusRoll < 0.6 ? 'ONGOING' : statusRoll < 0.9 ? 'COMPLETED' : 'PAUSED';
    // Vài truyện cuối danh sách ở trạng thái không công khai để thử các điều kiện hiển thị
    const visibility = index >= STORY_COUNT - 3 ? 'DRAFT' : index >= STORY_COUNT - 5 ? 'HIDDEN' : 'PUBLISHED';
    const ageDays = between(20, 120);
    const popularity = Math.exp((random() - 0.5) * 3);
    const story = {
      index, title, slug: slugify(title), hero, type, status, visibility, theme,
      author: AUTHORS[index % AUTHORS.length],
      description: pick(theme.descriptions).replaceAll('{hero}', hero),
      genres: sample(theme.genres, Math.min(theme.genres.length, between(2, 3))),
      ageDays, popularity, quality: 3 + random() * 2,
      chapters: [], follows: 0, ratingSum: 0, ratingCount: 0, commentCount: 0,
    };
    buildChapters(story);
    stories.push(story);
  }
  return stories;
}

function buildChapters(story) {
  const publishedTotal = story.type === 'NOVEL'
    ? between(story.status === 'COMPLETED' ? 18 : 8, 30)
    : between(story.status === 'COMPLETED' ? 10 : 5, 15);
  const interval = (story.ageDays * MINUTES_PER_DAY) / publishedTotal;
  // Truyện đã dừng thì chương cuối cũng đã lâu; truyện đang ra thì chương mới nhất chỉ cách vài giờ tới vài ngày
  const newestAgo = story.status === 'ONGOING' ? between(30, 4 * MINUTES_PER_DAY) : between(10, 25) * MINUTES_PER_DAY;
  for (let no = 1; no <= publishedTotal; no++) {
    const publishedAgo = newestAgo + (publishedTotal - no) * interval * ((story.ageDays * MINUTES_PER_DAY - newestAgo)
      / (story.ageDays * MINUTES_PER_DAY));
    story.chapters.push(buildChapter(story, no, 'PUBLISHED', publishedAgo));
  }
  if (story.status === 'ONGOING' && story.visibility === 'PUBLISHED') {
    if (story.index % 3 === 0) story.chapters.push(buildChapter(story, publishedTotal + 1, 'DRAFT', null));
    if (story.index % 4 === 0) story.chapters.push(buildChapter(story, publishedTotal + 2, 'SCHEDULED', null));
  }
}

function buildChapter(story, no, status, publishedAgo) {
  const chapter = { no, status, publishedAgo, title: CHAPTER_TITLES[(no + story.index) % CHAPTER_TITLES.length], views: 0 };
  if (story.type === 'NOVEL') {
    Object.assign(chapter, buildNovelContent(story.hero));
  } else {
    chapter.pages = Array.from({ length: between(6, 10) }, (_, i) => 1 + ((story.index + no + i) % PAGE_POOL_SIZE));
  }
  if (status === 'PUBLISHED') {
    // Chương đầu được xem nhiều nhất, giảm dần về sau
    chapter.views = Math.round(story.popularity * between(80, 400) * (0.4 + 0.6 / Math.sqrt(no)));
  }
  return chapter;
}

const publishedChapters = (story) => story.chapters.filter((chapter) => chapter.status === 'PUBLISHED');
const totalViews = (story) => publishedChapters(story).reduce((sum, chapter) => sum + chapter.views, 0);

function buildInteractions(stories) {
  const publicStories = stories.filter((story) => story.visibility === 'PUBLISHED');
  const weighted = (count) => {
    const pool = publicStories.flatMap((story) => Array(Math.max(1, Math.round(story.popularity * 4))).fill(story));
    return [...new Set(sample(pool, count * 3))].slice(0, count);
  };
  const follows = [];
  const ratings = [];
  const histories = [];
  for (const reader of READERS) {
    for (const story of weighted(between(4, 12))) {
      follows.push({ reader, story, ago: between(1, story.ageDays) * MINUTES_PER_DAY });
      story.follows++;
    }
    for (const story of weighted(between(5, 14))) {
      const stars = Math.max(1, Math.min(5, Math.round(story.quality + (random() - 0.5) * 2)));
      ratings.push({ reader, story, stars, ago: between(1, story.ageDays) * MINUTES_PER_DAY });
      story.ratingSum += stars;
      story.ratingCount++;
    }
    for (const story of weighted(between(3, 8))) {
      const chapters = publishedChapters(story);
      histories.push({ reader, story, chapterIndex: between(0, chapters.length - 1), ago: between(10, 20 * MINUTES_PER_DAY) });
    }
  }
  const comments = [];
  for (const story of weighted(40)) {
    for (let i = 0, total = between(2, 6); i < total; i++) {
      const comment = { reader: pick(READERS), story, content: pick(COMMENTS), ago: between(30, story.ageDays * MINUTES_PER_DAY), replies: [] };
      if (random() < 0.3) {
        comment.replies.push({ reader: pick(READERS), content: pick(REPLIES), ago: Math.max(5, comment.ago - between(10, 600)) });
      }
      story.commentCount += 1 + comment.replies.length;
      comments.push(comment);
    }
  }
  return { follows, ratings, histories, comments };
}

/** Rải lượt xem của truyện ra từng ngày gần đây; vài truyện được "đẩy" tuần này để bảng xếp hạng tuần khác bảng tổng. */
function buildDailyViews(story) {
  const days = Math.min(VIEW_HISTORY_DAYS, story.ageDays);
  const budget = Math.round(totalViews(story) * (days / story.ageDays));
  const trending = story.index % 7 === 0;
  const weights = Array.from({ length: days }, (_, day) => (0.5 + random()) * (trending && day < 7 ? 4 : 1));
  const weightSum = weights.reduce((sum, weight) => sum + weight, 0);
  return weights.map((weight, day) => ({ day, count: Math.round((budget * weight) / weightSum) })).filter((row) => row.count > 0);
}

// ---------- Ảnh giữ chỗ ----------
function wrapTitle(title) {
  const lines = [];
  let line = '';
  for (const word of title.split(' ')) {
    if ((line + ' ' + word).trim().length > 11 && line) { lines.push(line); line = word; } else { line = (line + ' ' + word).trim(); }
  }
  lines.push(line);
  return lines;
}

function coverSvg(story) {
  const [dark, light] = story.theme.color;
  const lines = wrapTitle(story.title);
  const firstY = 400 - (lines.length - 1) * 42;
  const titleSpans = lines.map((line, i) => `<text x="300" y="${firstY + i * 84}" class="t">${xml(line)}</text>`).join('');
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 800" width="600" height="800">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${dark}"/><stop offset="1" stop-color="${light}"/></linearGradient></defs>
<style>.t{font:700 64px sans-serif;fill:#fff;text-anchor:middle}.s{font:500 30px sans-serif;fill:#fff;fill-opacity:.85;text-anchor:middle}</style>
<rect width="600" height="800" fill="url(#g)"/>
<circle cx="500" cy="120" r="180" fill="#fff" fill-opacity=".08"/><circle cx="80" cy="720" r="140" fill="#fff" fill-opacity=".06"/>
<rect x="40" y="40" width="520" height="720" rx="18" fill="none" stroke="#fff" stroke-opacity=".35" stroke-width="3"/>
<text x="300" y="120" class="s">${story.type === 'COMIC' ? 'TRUYỆN TRANH' : 'TRUYỆN CHỮ'}</text>
${titleSpans}
<text x="300" y="700" class="s">${xml(story.author.penName)}</text>
</svg>
`;
}

/** Trang truyện tranh giữ chỗ: vài khung tranh xám với bố cục khác nhau theo số trang. */
function pageSvg(pageNo) {
  const layouts = [
    [[40, 40, 720, 520], [40, 600, 340, 560], [420, 600, 340, 560]],
    [[40, 40, 340, 360], [420, 40, 340, 360], [40, 440, 720, 360], [40, 840, 720, 320]],
    [[40, 40, 720, 300], [40, 380, 720, 420], [40, 840, 340, 320], [420, 840, 340, 320]],
    [[40, 40, 440, 540], [520, 40, 240, 540], [40, 620, 720, 540]],
  ];
  const panels = layouts[pageNo % layouts.length]
    .map(([x, y, w, h], i) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="6" fill="${i % 2 ? '#e5e7eb' : '#d1d5db'}" stroke="#111827" stroke-width="5"/>`)
    .join('');
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${PAGE_WIDTH} ${PAGE_HEIGHT}" width="${PAGE_WIDTH}" height="${PAGE_HEIGHT}">
<rect width="${PAGE_WIDTH}" height="${PAGE_HEIGHT}" fill="#fff"/>${panels}
<text x="400" y="1185" font-family="sans-serif" font-size="26" fill="#6b7280" text-anchor="middle">Trang giữ chỗ ${pageNo}</text>
</svg>
`;
}

function writeMedia(stories) {
  rmSync(MEDIA_DIR, { recursive: true, force: true });
  mkdirSync(join(MEDIA_DIR, 'covers'), { recursive: true });
  mkdirSync(join(MEDIA_DIR, 'pages'), { recursive: true });
  for (const story of stories) writeFileSync(join(MEDIA_DIR, 'covers', `${story.slug}.svg`), coverSvg(story));
  const pageSizes = {};
  for (let pageNo = 1; pageNo <= PAGE_POOL_SIZE; pageNo++) {
    const content = pageSvg(pageNo);
    writeFileSync(join(MEDIA_DIR, 'pages', pageFile(pageNo)), content);
    pageSizes[pageNo] = Buffer.byteLength(content);
  }
  return pageSizes;
}
const pageFile = (pageNo) => `page-${String(pageNo).padStart(2, '0')}.svg`;

// ---------- Xuất SQL ----------
const userVar = (user) => `@u_${user.username}`;
const storyVar = (story) => `@s${story.index}`;
const chapterVar = (story) => `@c${story.index}`;

function usersSql() {
  const lines = [];
  const newUsers = [...AUTHORS, ...READERS].filter((user) => !user.existing);
  lines.push('INSERT INTO user_account (username, email, password_hash, display_name, role) VALUES');
  lines.push(newUsers.map((user) => `  (${sql(user.username)}, ${sql(user.username + '@comic.local')}, ${sql(DEMO_PASSWORD_HASH)}, ${sql(user.displayName)}, ${sql(user.penName ? 'AUTHOR' : 'USER')})`).join(',\n') + ';');
  for (const author of AUTHORS.filter((user) => !user.existing)) {
    lines.push(`INSERT INTO author_profile (user_id, pen_name, bio, approved_at) SELECT id, ${sql(author.penName)}, ${sql(author.bio)}, ${minutesAgo(150 * MINUTES_PER_DAY)} FROM user_account WHERE username = ${sql(author.username)};`);
  }
  for (const user of [...AUTHORS, ...READERS]) {
    lines.push(`SET ${userVar(user)} = (SELECT id FROM user_account WHERE username = ${sql(user.username)});`);
  }
  return lines;
}

function storySql(story, pageSizes) {
  const lines = [];
  const published = publishedChapters(story);
  const visible = story.visibility !== 'DRAFT';
  lines.push(`INSERT INTO story (slug, title, description, cover_path, type, status, visibility, hidden_reason, author_id, chapter_count, view_count, follow_count, rating_sum, rating_count, comment_count, published_at, last_chapter_at, created_at) VALUES (${[
    sql(story.slug), sql(story.title), sql(story.description), sql(`demo/covers/${story.slug}.svg`), sql(story.type), sql(story.status),
    sql(story.visibility), sql(story.visibility === 'HIDDEN' ? HIDDEN_REASON : null), userVar(story.author), published.length,
    totalViews(story), story.follows, story.ratingSum, story.ratingCount, story.commentCount,
    visible ? minutesAgo(story.ageDays * MINUTES_PER_DAY) : 'NULL',
    minutesAgo(Math.min(...published.map((chapter) => chapter.publishedAgo))),
    minutesAgo((story.ageDays + 2) * MINUTES_PER_DAY),
  ].join(', ')});`);
  lines.push(`SET ${storyVar(story)} = LAST_INSERT_ID();`);
  lines.push(`INSERT INTO story_genre (story_id, genre_id) SELECT ${storyVar(story)}, id FROM genre WHERE slug IN (${story.genres.map(sql).join(', ')});`);
  lines.push('INSERT INTO chapter (story_id, chapter_no, title, status, scheduled_at, published_at, page_count, word_count, view_count) VALUES');
  lines.push(story.chapters.map((chapter) => `  (${[
    storyVar(story), chapter.no, sql(chapter.title), sql(chapter.status),
    chapter.status === 'SCHEDULED' ? 'NOW(6) + INTERVAL 3 DAY' : 'NULL',
    chapter.status === 'PUBLISHED' ? minutesAgo(chapter.publishedAgo) : 'NULL',
    chapter.pages ? chapter.pages.length : 0, chapter.wordCount ?? 0, chapter.views,
  ].join(', ')})`).join(',\n') + ';');
  // Một câu INSERT nhiều dòng cấp id liên tiếp, nên chương thứ k của truyện có id = id đầu + (k - 1)
  lines.push(`SET ${chapterVar(story)} = LAST_INSERT_ID();`);
  if (story.type === 'NOVEL') {
    lines.push('INSERT INTO chapter_content (chapter_id, content) VALUES');
    lines.push(story.chapters.map((chapter, i) => `  (${chapterVar(story)} + ${i}, ${sql(chapter.html)})`).join(',\n') + ';');
  } else {
    lines.push('INSERT INTO chapter_page (chapter_id, page_no, image_path, width, height, size_bytes) VALUES');
    lines.push(story.chapters.flatMap((chapter, i) => chapter.pages.map((pageNo, p) =>
      `  (${chapterVar(story)} + ${i}, ${p + 1}, ${sql(`demo/pages/${pageFile(pageNo)}`)}, ${PAGE_WIDTH}, ${PAGE_HEIGHT}, ${pageSizes[pageNo]})`)).join(',\n') + ';');
  }
  return lines;
}

function interactionsSql(stories, { follows, ratings, histories, comments }) {
  const lines = [];
  const today = "DATE(CONVERT_TZ(NOW(), '+00:00', '+07:00'))"; // ngày theo giờ Việt Nam, như ứng dụng ghi
  const dailyRows = stories.filter((story) => story.visibility === 'PUBLISHED')
    .flatMap((story) => buildDailyViews(story).map((row) => `  (${storyVar(story)}, ${today} - INTERVAL ${row.day} DAY, ${row.count})`));
  for (let i = 0; i < dailyRows.length; i += 500) {
    lines.push('INSERT INTO story_view_daily (story_id, view_date, view_count) VALUES');
    lines.push(dailyRows.slice(i, i + 500).join(',\n') + ';');
  }
  lines.push('INSERT INTO story_follow (user_id, story_id, created_at) VALUES');
  lines.push(follows.map((row) => `  (${userVar(row.reader)}, ${storyVar(row.story)}, ${minutesAgo(row.ago)})`).join(',\n') + ';');
  lines.push('INSERT INTO story_rating (user_id, story_id, stars, updated_at) VALUES');
  lines.push(ratings.map((row) => `  (${userVar(row.reader)}, ${storyVar(row.story)}, ${row.stars}, ${minutesAgo(row.ago)})`).join(',\n') + ';');
  lines.push('INSERT INTO reading_history (user_id, story_id, chapter_id, updated_at) VALUES');
  lines.push(histories.map((row) => `  (${userVar(row.reader)}, ${storyVar(row.story)}, ${chapterVar(row.story)} + ${row.chapterIndex}, ${minutesAgo(row.ago)})`).join(',\n') + ';');
  for (const comment of comments) {
    lines.push(`INSERT INTO comment (story_id, user_id, content, created_at) VALUES (${storyVar(comment.story)}, ${userVar(comment.reader)}, ${sql(comment.content)}, ${minutesAgo(comment.ago)});`);
    for (const reply of comment.replies) {
      lines.push(`INSERT INTO comment (story_id, user_id, parent_id, content, created_at) VALUES (${storyVar(comment.story)}, ${userVar(reply.reader)}, LAST_INSERT_ID(), ${sql(reply.content)}, ${minutesAgo(reply.ago)});`);
    }
  }
  return lines;
}

// ---------- Chạy ----------
const stories = buildStories();
const interactions = buildInteractions(stories);
const pageSizes = writeMedia(stories);
const chapterTotal = stories.reduce((sum, story) => sum + story.chapters.length, 0);
const header = [
  '-- Dữ liệu demo: truyện, chương, lượt xem, theo dõi, đánh giá, bình luận. Chỉ nạp ở profile dev.',
  '-- TỆP NÀY ĐƯỢC SINH TỰ ĐỘNG bởi backend/scripts/generate-demo-data.mjs — sửa script rồi sinh lại, đừng sửa tay.',
  `-- ${stories.length} truyện, ${chapterTotal} chương. Mật khẩu mọi tài khoản demo: Demo@123.`,
  '-- Mốc thời gian là tương đối so với lúc nạp. Ảnh bìa và trang truyện nằm ở resources/demo-media.',
  '',
];
writeFileSync(SQL_FILE, [
  ...header,
  ...usersSql(), '',
  ...stories.flatMap((story) => [...storySql(story, pageSizes), '']),
  ...interactionsSql(stories, interactions), '',
].join('\n'));
console.log(`Đã ghi ${stories.length} truyện, ${chapterTotal} chương, ${interactions.comments.length} luồng bình luận vào ${SQL_FILE}`);
