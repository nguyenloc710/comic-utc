/* Trang đọc: chuyển chương bằng phím ← →; cỡ chữ và nền sáng/tối cho truyện chữ (nhớ trong localStorage). */
(function () {
  'use strict';

  const reader = document.getElementById('reader');
  if (!reader) return;

  const FONT_KEY = 'reader.fontSize';
  const THEME_KEY = 'reader.dark';
  const FONT_MIN = 0.9;
  const FONT_MAX = 1.8;
  const FONT_STEP = 0.1;
  const FONT_DEFAULT = 1.125;

  /** Trình duyệt chặn localStorage (chế độ riêng tư) thì chỉ mất việc ghi nhớ, không làm hỏng trang. */
  function readSetting(key) {
    try { return localStorage.getItem(key); } catch (error) { return null; }
  }

  function saveSetting(key, value) {
    try { localStorage.setItem(key, value); } catch (error) { /* bỏ qua, xem ghi chú ở readSetting */ }
  }

  function initKeyboardNavigation() {
    document.addEventListener('keydown', function (event) {
      // Đang gõ bình luận thì phím mũi tên là để di chuyển con trỏ, không phải để chuyển chương
      if (event.target.closest('input, textarea, select') || event.altKey || event.ctrlKey || event.metaKey) return;
      const direction = event.key === 'ArrowLeft' ? 'previous' : event.key === 'ArrowRight' ? 'next' : null;
      if (!direction) return;
      const link = reader.querySelector('[data-reader-nav="' + direction + '"]');
      if (link) window.location.href = link.href;
    });
  }

  /** Ô chọn chương: đổi lựa chọn là mở chương đó. */
  function initChapterSelect() {
    document.querySelectorAll('[data-reader-select]').forEach(function (select) {
      select.addEventListener('change', function () { window.location.href = select.value; });
    });
  }

  function initFontSize() {
    let size = Number(readSetting(FONT_KEY)) || FONT_DEFAULT;

    function apply() {
      reader.style.setProperty('--reader-font-size', size + 'rem');
    }

    reader.querySelectorAll('[data-reader-font]').forEach(function (button) {
      button.addEventListener('click', function () {
        const next = size + Number(button.dataset.readerFont) * FONT_STEP;
        size = Math.min(FONT_MAX, Math.max(FONT_MIN, Math.round(next * 100) / 100));
        saveSetting(FONT_KEY, String(size));
        apply();
      });
    });
    apply();
  }

  function initTheme() {
    const toggle = reader.querySelector('[data-reader-theme]');
    if (!toggle) return;
    reader.classList.toggle('reader-dark', readSetting(THEME_KEY) === '1');
    toggle.addEventListener('click', function () {
      const dark = reader.classList.toggle('reader-dark');
      saveSetting(THEME_KEY, dark ? '1' : '0');
    });
  }

  initKeyboardNavigation();
  initChapterSelect();
  initFontSize();
  initTheme();
})();
