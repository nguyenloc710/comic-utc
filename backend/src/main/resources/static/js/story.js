/* Trang chi tiết truyện: nút theo dõi và các ngôi sao đánh giá. Nhãn hiển thị lấy từ thuộc tính data-label-*. */
(function () {
  'use strict';

  const errorBox = document.getElementById('interactionError');

  function showError(error) {
    if (!errorBox) return;
    errorBox.textContent = error.message;
    errorBox.classList.remove('d-none');
  }

  function clearError() {
    if (errorBox) errorBox.classList.add('d-none');
  }

  function formatInteger(value) {
    return Number(value).toLocaleString('vi-VN');
  }

  function initFollowButton() {
    const button = document.getElementById('followButton');
    if (!button) return;
    const icon = button.querySelector('i');
    const label = button.querySelector('span');
    const followCount = document.getElementById('followCount');

    function render(following) {
      button.dataset.following = String(following);
      icon.className = 'bi ' + (following ? 'bi-bookmark-check-fill' : 'bi-bookmark-plus');
      label.textContent = following ? button.dataset.labelUnfollow : button.dataset.labelFollow;
    }

    button.addEventListener('click', function () {
      // PUT để theo dõi, DELETE để bỏ: cả hai đều lặp lại được nên bấm đúp không làm lệch số người theo dõi
      const method = button.dataset.following === 'true' ? 'DELETE' : 'PUT';
      button.disabled = true;
      clearError();
      App.fetchJson('/api/stories/' + button.dataset.storyId + '/follow', { method: method })
        .then(function (result) {
          render(result.following);
          if (followCount) followCount.textContent = formatInteger(result.followCount);
        })
        .catch(showError)
        .finally(function () { button.disabled = false; });
    });
  }

  function initRatingWidget() {
    const widget = document.getElementById('ratingWidget');
    if (!widget) return;
    const stars = Array.from(widget.querySelectorAll('.rating-star'));
    const summary = document.getElementById('ratingSummary');

    function renderStars(myStars) {
      stars.forEach(function (star) {
        star.querySelector('i').className = 'bi ' + (Number(star.dataset.stars) <= myStars ? 'bi-star-fill' : 'bi-star');
      });
    }

    function renderSummary(result) {
      if (!summary) return;
      const value = summary.querySelector('.story-rating-value');
      const count = summary.querySelector('.story-rating-count');
      const average = result.ratingAverage.toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
      if (value) {
        value.textContent = average;
      } else if (count) {
        // Lần đánh giá đầu tiên của truyện: trước đó chưa có ô hiển thị điểm trung bình
        const created = document.createElement('span');
        created.className = 'story-rating-value';
        created.textContent = average;
        count.before(created, ' ');
      }
      if (count) count.textContent = widget.dataset.labelCount.replace('{0}', formatInteger(result.ratingCount));
    }

    stars.forEach(function (star) {
      star.addEventListener('click', function () {
        clearError();
        App.fetchJson('/api/stories/' + widget.dataset.storyId + '/rating', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ stars: Number(star.dataset.stars) })
        }).then(function (result) {
          renderStars(result.myStars);
          renderSummary(result);
        }).catch(showError);
      });
    });
  }

  initFollowButton();
  initRatingWidget();
})();
