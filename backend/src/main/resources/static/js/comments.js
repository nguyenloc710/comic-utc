/*
 * Khu bình luận (fragments/comments.html): tải danh sách từ /api/comments, gửi bình luận, trả lời, xóa.
 * Mọi nội dung do người dùng gõ đều được chèn bằng textContent, không bao giờ qua innerHTML.
 * Nhãn hiển thị lấy từ thuộc tính data-label-* của phần tử gốc.
 */
(function () {
  'use strict';

  const root = document.getElementById('comments');
  if (!root) return;

  const labels = root.dataset;
  const list = root.querySelector('[data-comment-list]');
  const emptyNote = root.querySelector('[data-comment-empty]');
  const moreButton = root.querySelector('[data-comment-more]');
  const errorBox = root.querySelector('[data-comment-error]');
  const form = root.querySelector('.comment-form');
  const authenticated = labels.authenticated === 'true';
  let nextPage = 0;

  function showError(error) {
    errorBox.textContent = error.message || labels.labelLoadError;
    errorBox.classList.remove('d-none');
  }

  function clearError() {
    errorBox.classList.add('d-none');
  }

  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  }

  function buildAvatar(comment) {
    if (comment.authorAvatarUrl) {
      const image = element('img', 'user-avatar');
      image.src = comment.authorAvatarUrl;
      image.alt = '';
      return image;
    }
    return element('span', 'user-avatar', comment.authorName.trim().charAt(0).toUpperCase());
  }

  function buildContent(comment) {
    if (comment.status === 'VISIBLE') {
      return element('div', 'comment-content', comment.content);
    }
    return element('div', 'comment-content comment-placeholder',
      comment.status === 'HIDDEN' ? labels.labelHidden : labels.labelDeleted);
  }

  /** Nút "Trả lời" chỉ có ở bình luận gốc đang hiển thị; nút "Xóa" chỉ có ở bình luận của chính mình. */
  function buildActions(comment, replies) {
    const actions = element('div', 'd-flex gap-3');
    if (authenticated && comment.parentId === null && comment.status === 'VISIBLE') {
      const reply = element('button', 'comment-action', labels.labelReply);
      reply.type = 'button';
      reply.addEventListener('click', function () { toggleReplyForm(comment, replies); });
      actions.append(reply);
    }
    if (comment.mine && comment.status === 'VISIBLE') {
      const remove = element('button', 'comment-action', labels.labelDelete);
      remove.type = 'button';
      remove.addEventListener('click', function () { deleteComment(comment); });
      actions.append(remove);
    }
    return actions;
  }

  function buildComment(comment) {
    const item = element('div', 'comment-item');
    item.dataset.commentId = comment.id;
    const body = element('div', 'comment-body');
    const header = element('div', 'd-flex flex-wrap align-items-baseline gap-2');
    header.append(
      element('span', 'fw-semibold', comment.authorName),
      element('span', 'small text-secondary', new Date(comment.createdAt).toLocaleString('vi-VN')));
    const replies = element('div', comment.parentId === null ? 'comment-replies d-none' : 'd-none');
    body.append(header, buildContent(comment), buildActions(comment, replies), replies);
    (comment.replies || []).forEach(function (reply) { appendReply(replies, reply); });
    item.append(buildAvatar(comment), body);
    return item;
  }

  function appendReply(replies, reply) {
    replies.classList.remove('d-none');
    replies.append(buildComment(reply));
  }

  function toggleReplyForm(comment, replies) {
    const existing = replies.querySelector('.comment-reply-form');
    if (existing) {
      existing.remove();
      return;
    }
    const replyForm = element('form', 'comment-reply-form mb-2');
    const input = element('textarea', 'form-control form-control-sm mb-2');
    input.rows = 2;
    input.maxLength = 1000;
    input.required = true;
    input.placeholder = labels.labelReplyPlaceholder;
    const send = element('button', 'btn btn-primary btn-sm me-2', labels.labelSend);
    send.type = 'submit';
    const cancel = element('button', 'btn btn-outline-secondary btn-sm', labels.labelCancel);
    cancel.type = 'button';
    cancel.addEventListener('click', function () { replyForm.remove(); });
    replyForm.append(input, send, cancel);
    replyForm.addEventListener('submit', function (event) {
      event.preventDefault();
      submitComment(input.value, comment.id, send).then(function (created) {
        if (!created) return;
        replyForm.remove();
        appendReply(replies, created);
      });
    });
    replies.classList.remove('d-none');
    replies.prepend(replyForm);
    input.focus();
  }

  /** Gửi bình luận; trả về bình luận vừa tạo, hoặc null nếu máy chủ từ chối (lỗi đã được hiện ra). */
  function submitComment(content, parentId, submitButton) {
    clearError();
    submitButton.disabled = true;
    return App.fetchJson('/api/comments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        storyId: Number(labels.storyId),
        chapterId: labels.chapterId ? Number(labels.chapterId) : null,
        parentId: parentId,
        content: content
      })
    }).catch(function (error) {
      showError(error);
      return null;
    }).finally(function () { submitButton.disabled = false; });
  }

  function deleteComment(comment) {
    if (!window.confirm(labels.labelDeleteConfirm)) return;
    clearError();
    App.fetchJson('/api/comments/' + comment.id, { method: 'DELETE' }).then(function () {
      // Bình luận đã xóa vẫn giữ chỗ trong luồng để các câu trả lời không mất ngữ cảnh
      const item = root.querySelector('[data-comment-id="' + comment.id + '"]');
      const deleted = Object.assign({}, comment, { status: 'DELETED', content: null, mine: false, replies: [] });
      const replacement = buildComment(deleted);
      const oldReplies = item.querySelector('.comment-replies');
      if (oldReplies) replacement.querySelector('.comment-body').lastChild.replaceWith(oldReplies);
      item.replaceWith(replacement);
    }).catch(showError);
  }

  function loadPage() {
    const query = new URLSearchParams({ storyId: labels.storyId, page: String(nextPage) });
    if (labels.chapterId) query.set('chapterId', labels.chapterId);
    App.fetchJson('/api/comments?' + query.toString()).then(function (page) {
      page.content.forEach(function (comment) { list.append(buildComment(comment)); });
      nextPage = page.page + 1;
      moreButton.classList.toggle('d-none', !page.hasNext);
      emptyNote.classList.toggle('d-none', list.childElementCount > 0);
    }).catch(showError);
  }

  if (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      const input = form.querySelector('textarea');
      submitComment(input.value, null, form.querySelector('button[type="submit"]')).then(function (created) {
        if (!created) return;
        input.value = '';
        list.prepend(buildComment(created));
        emptyNote.classList.add('d-none');
      });
    });
  }
  moreButton.addEventListener('click', loadPage);
  loadPage();
})();
