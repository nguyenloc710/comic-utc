/* Biểu tượng chuông: hỏi lại số thông báo chưa đọc theo chu kỳ và cập nhật con số trên chuông. */
(function () {
  'use strict';

  const POLL_INTERVAL_MS = 60000;
  const MAX_DISPLAY_COUNT = 99;

  const bell = document.querySelector('[data-notification-bell]');
  if (!bell || !window.App) {
    return;
  }
  const badge = bell.querySelector('[data-notification-count]');

  function render(count) {
    badge.textContent = count > MAX_DISPLAY_COUNT ? MAX_DISPLAY_COUNT + '+' : String(count);
    badge.classList.toggle('d-none', count === 0);
  }

  async function refresh() {
    try {
      const data = await window.App.fetchJson(bell.dataset.countUrl);
      render(data.count);
    } catch (error) {
      // Mất mạng hoặc hết phiên: giữ nguyên con số đang hiện, lượt hỏi sau sẽ thử lại
    }
  }

  refresh();
  window.setInterval(refresh, POLL_INTERVAL_MS);
})();
