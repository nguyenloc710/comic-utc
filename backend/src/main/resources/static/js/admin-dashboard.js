/* Trang tổng quan quản trị: bốn biểu đồ tải một lần từ API. */
(function () {
  'use strict';

  const root = document.querySelector('[data-admin-charts]');
  if (!root || !window.App || !window.Chart) {
    return;
  }
  const errorNote = root.querySelector('[data-charts-error]');
  const styles = getComputedStyle(document.documentElement);
  const PRIMARY = styles.getPropertyValue('--comic-primary').trim();
  const SECONDARY = styles.getPropertyValue('--comic-primary-dark').trim();
  const LOCALE = document.documentElement.lang || undefined;

  function shortDate(isoDate) {
    const parts = isoDate.split('-');
    return parts[2] + '/' + parts[1];
  }

  function lineChart(selector, points, label, color) {
    new window.Chart(root.querySelector(selector), {
      type: 'line',
      data: {
        labels: points.map(function (point) { return shortDate(point.date); }),
        datasets: [{ label: label, data: points.map(function (point) { return point.views; }),
          borderColor: color, backgroundColor: color, tension: 0.3, pointRadius: 2 }]
      },
      options: { locale: LOCALE, maintainAspectRatio: false, plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
  }

  function barChart(selector, items, label, color, horizontal) {
    new window.Chart(root.querySelector(selector), {
      type: 'bar',
      data: {
        labels: items.map(function (item) { return item.name; }),
        datasets: [{ label: label, data: items.map(function (item) { return item.count; }), backgroundColor: color }]
      },
      options: { locale: LOCALE, maintainAspectRatio: false, indexAxis: horizontal ? 'y' : 'x',
        plugins: { legend: { display: false } },
        scales: { x: { beginAtZero: true, ticks: { precision: 0 } }, y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
  }

  window.App.fetchJson(root.dataset.url).then(function (charts) {
    lineChart('[data-chart-registrations]', charts.registrations, root.dataset.labelRegistrations, SECONDARY);
    lineChart('[data-chart-views]', charts.views, root.dataset.labelViews, PRIMARY);
    barChart('[data-chart-genres]', charts.genres, root.dataset.labelStories, PRIMARY, false);
    barChart('[data-chart-top]', charts.topStories, root.dataset.labelViews, SECONDARY, true);
  }).catch(function () {
    errorNote.classList.remove('d-none');
  });
})();
