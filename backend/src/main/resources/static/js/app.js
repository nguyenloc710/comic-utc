/* Tiện ích dùng chung cho mọi trang. Chức năng từng trang nằm ở tệp JS riêng và gọi qua window.App. */
(function () {
  'use strict';

  const csrfToken = document.querySelector('meta[name="_csrf"]');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]');

  /**
   * fetch() tới /api/** tự gắn CSRF header; trả về trường data của ApiResponse,
   * ném Error mang message đã dịch khi máy chủ báo lỗi.
   */
  async function fetchJson(url, options) {
    const opts = Object.assign({}, options || {});
    opts.headers = Object.assign({ Accept: 'application/json' }, opts.headers || {});
    if (csrfToken && csrfHeader) {
      opts.headers[csrfHeader.content] = csrfToken.content;
    }
    const response = await fetch(url, opts);
    const body = await response.json();
    if (!response.ok || body.success === false) {
      const error = new Error(body.message || response.statusText);
      error.code = body.errorCode;
      error.fieldErrors = body.errors || [];
      throw error;
    }
    return body.data;
  }

  /**
   * Form có data-confirm phải được người dùng xác nhận trước khi gửi (khóa tài khoản, xóa thể loại...).
   * Làm ở đây thay vì onclick trong HTML vì CSP của trang cấm script inline.
   */
  function initConfirmForms() {
    document.addEventListener('submit', function (event) {
      const message = event.target.dataset ? event.target.dataset.confirm : null;
      if (message && !window.confirm(message)) {
        event.preventDefault();
      }
    });
  }

  initConfirmForms();

  window.App = { fetchJson: fetchJson };
})();
