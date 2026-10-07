/* Hộp thoại báo cáo vi phạm (fragments/report.html): mở cho một đối tượng, gửi tới /api/reports. */
(function () {
  'use strict';

  const modalElement = document.getElementById('reportModal');
  if (!modalElement || !window.App || !window.bootstrap) {
    return;
  }
  const modal = window.bootstrap.Modal.getOrCreateInstance(modalElement);
  const form = modalElement.querySelector('[data-report-form]');
  const errorBox = modalElement.querySelector('[data-report-error]');
  const successBox = modalElement.querySelector('[data-report-success]');
  const submitButton = form.querySelector('button[type="submit"]');
  let target = null;

  /** Mở hộp thoại cho một nội dung; comments.js cũng gọi hàm này cho từng bình luận. */
  function openReport(targetType, targetId) {
    target = { targetType: targetType, targetId: Number(targetId) };
    form.reset();
    errorBox.classList.add('d-none');
    successBox.classList.add('d-none');
    submitButton.disabled = false;
    modal.show();
  }

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    if (!target) {
      return;
    }
    submitButton.disabled = true;
    errorBox.classList.add('d-none');
    try {
      await window.App.fetchJson('/api/reports', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetType: target.targetType,
          targetId: target.targetId,
          reason: form.elements.reason.value,
          detail: form.elements.detail.value
        })
      });
      successBox.textContent = modalElement.dataset.labelSent;
      successBox.classList.remove('d-none');
      window.setTimeout(function () { modal.hide(); }, 1500);
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.classList.remove('d-none');
      submitButton.disabled = false;
    }
  });

  document.querySelectorAll('[data-report-button]').forEach(function (button) {
    button.addEventListener('click', function () {
      openReport(button.dataset.targetType, button.dataset.targetId);
    });
  });

  window.App.openReport = openReport;
})();
