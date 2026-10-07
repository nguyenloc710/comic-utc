/* Trang thống kê của tác giả: biểu đồ lượt xem theo ngày, đổi truyện thì tải lại dữ liệu. */
(function () {
  'use strict';

  const root = document.querySelector('[data-stats-chart]');
  if (!root || !window.App || !window.Chart) {
    return;
  }
  const storySelect = root.querySelector('[data-stats-story]');
  const errorNote = root.querySelector('[data-stats-error]');
  // Lấy màu thương hiệu từ biến CSS của trang để biểu đồ đổi màu theo giao diện, không chép mã màu sang đây
  const LINE_COLOR = getComputedStyle(document.documentElement).getPropertyValue('--comic-primary').trim();

  const chart = new window.Chart(root.querySelector('[data-stats-canvas]'), {
    type: 'line',
    data: {
      labels: [],
      datasets: [{
        label: root.dataset.label,
        data: [],
        borderColor: LINE_COLOR,
        backgroundColor: LINE_COLOR,
        fill: false,
        tension: 0.3,
        pointRadius: 2
      }]
    },
    options: {
      // Định dạng số trên trục theo ngôn ngữ của trang (1.500 thay vì 1,500)
      locale: document.documentElement.lang || undefined,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
    }
  });

  /** "2026-11-05" → "05/11": đủ để đọc trục ngang của 30 ngày. */
  function shortDate(isoDate) {
    const parts = isoDate.split('-');
    return parts[2] + '/' + parts[1];
  }

  async function load() {
    const storyId = storySelect.value;
    const url = storyId ? root.dataset.url + '?storyId=' + encodeURIComponent(storyId) : root.dataset.url;
    try {
      const points = await window.App.fetchJson(url);
      chart.data.labels = points.map(function (point) { return shortDate(point.date); });
      chart.data.datasets[0].data = points.map(function (point) { return point.views; });
      chart.update();
      errorNote.classList.add('d-none');
    } catch (error) {
      errorNote.textContent = root.dataset.error;
      errorNote.classList.remove('d-none');
    }
  }

  storySelect.addEventListener('change', load);
  load();
})();
