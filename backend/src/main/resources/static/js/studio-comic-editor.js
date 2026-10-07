/* Trình soạn chương truyện tranh: tải ảnh lên từng tấm, kéo thả đổi thứ tự, xóa ảnh. */
(function () {
  'use strict';

  const root = document.querySelector('[data-comic-editor]');
  if (!root || !window.App) {
    return;
  }
  const pagesUrl = root.dataset.pagesUrl;
  const input = root.querySelector('[data-page-input]');
  const grid = root.querySelector('[data-page-grid]');
  const emptyNote = root.querySelector('[data-page-empty]');
  const progress = root.querySelector('[data-upload-progress]');
  const progressBar = root.querySelector('[data-upload-bar]');
  const errorList = root.querySelector('[data-upload-errors]');

  function pageLabel(pageNo) {
    return root.dataset.msgPage.replace('{0}', pageNo);
  }

  function buildPageItem(page) {
    const item = document.createElement('div');
    item.className = 'comic-page';
    item.dataset.pageId = page.id;

    const image = document.createElement('img');
    image.src = page.imageUrl;
    image.alt = pageLabel(page.pageNo);
    image.loading = 'lazy';

    const number = document.createElement('span');
    number.className = 'comic-page-number badge text-bg-dark';
    number.textContent = page.pageNo;

    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'comic-page-remove btn btn-danger btn-sm';
    remove.setAttribute('aria-label', root.dataset.msgRemove);
    const icon = document.createElement('i');
    icon.className = 'bi bi-x-lg';
    remove.appendChild(icon);
    remove.addEventListener('click', function () { removePage(page.id); });

    item.append(image, number, remove);
    return item;
  }

  function render(pages) {
    grid.replaceChildren(...pages.map(buildPageItem));
    emptyNote.classList.toggle('d-none', pages.length > 0);
  }

  function showError(message) {
    const line = document.createElement('li');
    line.textContent = message;
    errorList.appendChild(line);
  }

  async function loadPages() {
    try {
      render(await window.App.fetchJson(pagesUrl));
    } catch (error) {
      showError(error.message);
    }
  }

  async function removePage(pageId) {
    if (!window.confirm(root.dataset.msgDeleteConfirm)) {
      return;
    }
    errorList.replaceChildren();
    try {
      render(await window.App.fetchJson(pagesUrl + '/' + pageId, { method: 'DELETE' }));
    } catch (error) {
      showError(error.message);
    }
  }

  /** Gửi thứ tự mới sau khi kéo thả; máy chủ từ chối thì nạp lại để màn hình khớp với dữ liệu thật. */
  async function saveOrder() {
    const pageIds = Array.from(grid.children).map(function (item) { return Number(item.dataset.pageId); });
    errorList.replaceChildren();
    try {
      render(await window.App.fetchJson(pagesUrl + '/order', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pageIds: pageIds })
      }));
    } catch (error) {
      showError(error.message);
      await loadPages();
    }
  }

  function setProgress(done, total) {
    const percent = Math.round((done / total) * 100);
    progress.setAttribute('aria-valuenow', percent);
    progressBar.style.width = percent + '%';
    progressBar.textContent = done + ' / ' + total;
  }

  /**
   * Tải lên TỪNG ảnh một request, lần lượt: máy chủ xếp trang theo thứ tự nhận được, nên phải sắp tên tệp theo
   * thứ tự tự nhiên trước ("2.jpg" đứng trước "10.jpg") và không gửi song song.
   */
  async function uploadFiles(fileList) {
    const files = Array.from(fileList).sort(function (first, second) {
      return first.name.localeCompare(second.name, undefined, { numeric: true, sensitivity: 'base' });
    });
    if (files.length === 0) {
      return;
    }
    errorList.replaceChildren();
    input.disabled = true;
    progress.classList.remove('d-none');
    setProgress(0, files.length);
    for (let index = 0; index < files.length; index++) {
      const body = new FormData();
      body.append('file', files[index]);
      try {
        await window.App.fetchJson(pagesUrl, { method: 'POST', body: body });
      } catch (error) {
        showError(files[index].name + ': ' + error.message);
      }
      setProgress(index + 1, files.length);
    }
    input.value = '';
    input.disabled = false;
    await loadPages();
  }

  input.addEventListener('change', function () { uploadFiles(input.files); });
  if (window.Sortable) {
    window.Sortable.create(grid, { animation: 150, onEnd: saveOrder });
  }
  loadPages();
})();
