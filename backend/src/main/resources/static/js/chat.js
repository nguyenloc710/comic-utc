/*
 * Khung chat gợi ý truyện (fragments/chat-widget.html). Gọi /api/chat/**; mọi nội dung (lời người dùng, lời trợ
 * lý, tên truyện) đều chèn bằng textContent, không bao giờ qua innerHTML.
 */
(function () {
  'use strict';

  const root = document.getElementById('chatWidget');
  if (!root || !window.App) {
    return;
  }
  const labels = root.dataset;
  const panel = root.querySelector('#chatPanel');
  const toggle = root.querySelector('[data-chat-toggle]');
  const list = root.querySelector('[data-chat-messages]');
  const welcome = root.querySelector('[data-chat-welcome]');
  const typing = root.querySelector('[data-chat-typing]');
  const errorBox = root.querySelector('[data-chat-error]');
  const form = root.querySelector('[data-chat-form]');
  const input = form.querySelector('textarea');
  const sendButton = form.querySelector('button[type="submit"]');
  let conversationId = null;
  let loaded = false;

  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  }

  function scrollToBottom() {
    list.scrollTop = list.scrollHeight;
  }

  function showError(message) {
    errorBox.textContent = message || labels.labelError;
    errorBox.classList.remove('d-none');
  }

  /** Thẻ truyện: mọi thông tin đến từ máy chủ (dựng từ cơ sở dữ liệu), chỉ lý do là lời của mô hình. */
  function buildCard(card) {
    const story = card.story;
    const link = element('a', 'chat-card');
    link.href = '/stories/' + encodeURIComponent(story.slug);
    const cover = element('div', 'chat-card-cover');
    if (story.coverUrl) {
      const image = element('img');
      image.src = story.coverUrl;
      image.alt = '';
      image.loading = 'lazy';
      cover.append(image);
    }
    const body = element('div', 'chat-card-body');
    body.append(element('div', 'chat-card-title', story.title));
    body.append(element('div', 'chat-card-meta', labels.labelChapters.replace('{0}', story.chapterCount)));
    if (card.reason) {
      body.append(element('div', 'chat-card-reason', card.reason));
    }
    link.append(cover, body);
    return link;
  }

  function buildFeedback(message) {
    const bar = element('div', 'chat-feedback');
    [[1, 'bi-hand-thumbs-up', labels.labelHelpful], [-1, 'bi-hand-thumbs-down', labels.labelUnhelpful]]
      .forEach(function (option) {
        const button = element('button', 'btn btn-link btn-sm p-0');
        button.type = 'button';
        button.setAttribute('aria-label', option[2]);
        button.setAttribute('aria-pressed', String(message.feedback === option[0]));
        button.append(element('i', 'bi ' + option[1] + (message.feedback === option[0] ? '-fill' : '')));
        button.addEventListener('click', function () { sendFeedback(message, option[0], bar); });
        bar.append(button);
      });
    return bar;
  }

  function appendMessage(message) {
    welcome.classList.add('d-none');
    const mine = message.role === 'USER';
    const bubble = element('div', 'chat-bubble ' + (mine ? 'chat-bubble-user' : 'chat-bubble-assistant'));
    bubble.append(element('div', 'chat-text', message.content));
    if (!mine && message.source === 'FALLBACK_KEYWORD') {
      bubble.append(element('div', 'chat-fallback-badge', labels.labelFallback));
    }
    if (message.cards && message.cards.length > 0) {
      const cards = element('div', 'chat-cards');
      message.cards.forEach(function (card) { cards.append(buildCard(card)); });
      bubble.append(cards);
    }
    if (!mine && message.id) {
      bubble.append(buildFeedback(message));
    }
    list.append(bubble);
    scrollToBottom();
  }

  async function sendFeedback(message, value, bar) {
    const next = message.feedback === value ? 0 : value;
    try {
      await window.App.fetchJson('/api/chat/messages/' + message.id + '/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ value: next })
      });
      message.feedback = next === 0 ? null : next;
      bar.replaceWith(buildFeedback(message));
    } catch (error) {
      showError(error.message);
    }
  }

  async function send(content) {
    const text = content.trim();
    if (!text) return;
    errorBox.classList.add('d-none');
    appendMessage({ role: 'USER', content: text });
    input.value = '';
    sendButton.disabled = true;
    typing.classList.remove('d-none');
    try {
      const reply = await window.App.fetchJson('/api/chat/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ conversationId: conversationId, content: text })
      });
      conversationId = reply.conversationId;
      appendMessage({ id: reply.messageId, role: 'ASSISTANT', content: reply.reply, cards: reply.cards,
        source: reply.source, feedback: null });
    } catch (error) {
      showError(error.message);
    } finally {
      typing.classList.add('d-none');
      sendButton.disabled = false;
      input.focus();
    }
  }

  /** Mở lần đầu: nạp lại hội thoại gần nhất để người dùng hỏi nối tiếp được sau khi chuyển trang. */
  async function loadLatestConversation() {
    loaded = true;
    try {
      const conversations = await window.App.fetchJson('/api/chat/conversations');
      if (conversations.length === 0) return;
      conversationId = conversations[0].id;
      const messages = await window.App.fetchJson('/api/chat/conversations/' + conversationId + '/messages');
      messages.forEach(appendMessage);
    } catch (error) {
      showError(error.message);
    }
  }

  function startNewConversation() {
    conversationId = null;
    Array.from(list.children).forEach(function (child) {
      if (child !== welcome) child.remove();
    });
    welcome.classList.remove('d-none');
    errorBox.classList.add('d-none');
    input.focus();
  }

  function setOpen(open) {
    panel.classList.toggle('d-none', !open);
    toggle.setAttribute('aria-expanded', String(open));
    if (open) {
      if (!loaded) loadLatestConversation();
      input.focus();
    }
  }

  toggle.addEventListener('click', function () { setOpen(panel.classList.contains('d-none')); });
  root.querySelector('[data-chat-close]').addEventListener('click', function () { setOpen(false); });
  root.querySelector('[data-chat-new]').addEventListener('click', startNewConversation);
  root.querySelectorAll('[data-chat-suggestion]').forEach(function (button) {
    button.addEventListener('click', function () { send(button.textContent); });
  });
  form.addEventListener('submit', function (event) {
    event.preventDefault();
    send(input.value);
  });
  // Enter gửi, Shift+Enter xuống dòng
  input.addEventListener('keydown', function (event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      if (!sendButton.disabled) send(input.value);
    }
  });
})();
