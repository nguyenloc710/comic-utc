/* Trình soạn chương truyện chữ (Quill). Máy chủ vẫn làm sạch HTML lúc lưu; giới hạn ở đây chỉ để thanh công cụ gọn. */
(function () {
  'use strict';

  const form = document.querySelector('[data-novel-form]');
  if (!form || !window.Quill) {
    return;
  }
  const contentField = form.querySelector('[data-novel-content]');
  const editorElement = form.querySelector('[data-novel-quill]');

  // Đúng các định dạng mà máy chủ giữ lại (docs/00 §4.4); định dạng khác dán vào sẽ bị bỏ ngay ở trình soạn thảo
  const quill = new window.Quill(editorElement, {
    theme: 'snow',
    placeholder: editorElement.dataset.placeholder || '',
    formats: ['header', 'bold', 'italic', 'underline', 'blockquote'],
    modules: {
      toolbar: [[{ header: [2, 3, false] }], ['bold', 'italic', 'underline'], ['blockquote'], ['clean']]
    }
  });

  // Nội dung đã lưu nằm trong textarea ẩn dưới dạng text; Quill tự chuyển nó về các định dạng được phép
  if (contentField.value.trim() !== '') {
    quill.clipboard.dangerouslyPasteHTML(contentField.value);
  }

  form.addEventListener('submit', function () {
    contentField.value = quill.getText().trim() === '' ? '' : quill.root.innerHTML;
  });
})();
