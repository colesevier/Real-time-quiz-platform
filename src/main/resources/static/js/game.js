(function () {
  const els = {};
  const state = {
    code: '',
    role: '',
    nickname: '',
    send: null,
    countdownId: null,
    answerLocked: false,
    finalPayload: null
  };

  function init(options) {
    state.code = options.code;
    state.role = options.role;
    state.nickname = options.nickname || '';
    state.send = options.send;

    [
      'game-panel', 'game-status', 'question-count', 'timer', 'question-panel',
      'question-prompt', 'choice-list', 'leaderboard-panel', 'leaderboard-list',
      'final-panel', 'podium', 'session-summary', 'next-question-btn',
      'finish-game-btn', 'answer-feedback', 'export-results-btn'
    ].forEach(function (id) {
      els[id] = document.getElementById(id);
    });

    if (els['next-question-btn']) {
      els['next-question-btn'].addEventListener('click', function () {
        state.send('/app/game/' + state.code + '/next', {});
        els['next-question-btn'].disabled = true;
      });
    }
    if (els['finish-game-btn']) {
      els['finish-game-btn'].addEventListener('click', function () {
        state.send('/app/game/' + state.code + '/finish', {});
      });
    }
    if (els['export-results-btn']) {
      els['export-results-btn'].addEventListener('click', exportResultsCsv);
    }
  }

  function handleEvent(ev) {
    if (!ev || !ev.type) return false;
    switch (ev.type) {
      case 'started':
        showGamePanel();
        setText(els['game-status'], 'Live Game');
        return false;
      case 'question_started':
        renderQuestion(ev.payload || {});
        return true;
      case 'leaderboard_update':
        renderLeaderboard(ev.payload || {});
        return true;
      case 'final_results':
        renderFinalResults(ev.payload || {});
        return true;
      case 'answer_received':
      case 'answer_rejected':
        renderAnswerAck(ev.type, ev.payload || {});
        return true;
      default:
        return false;
    }
  }

  function showGamePanel() {
    show(els['game-panel']);
  }

  function renderQuestion(payload) {
    showGamePanel();
    hide(els['leaderboard-panel']);
    hide(els['final-panel']);
    show(els['question-panel']);
    state.answerLocked = false;

    setText(els['game-status'], 'Question Open');
    setText(els['question-count'], 'Question ' + payload.questionNumber + ' of ' + payload.totalQuestions);
    setText(els['question-prompt'], payload.prompt || '');
    setText(els['answer-feedback'], '');

    if (els['next-question-btn']) {
      els['next-question-btn'].classList.add('hidden');
      els['next-question-btn'].disabled = true;
    }
    if (els['finish-game-btn']) {
      els['finish-game-btn'].classList.remove('hidden');
    }

    if (state.role === 'player') {
      renderChoices(payload.choices || {});
    } else {
      renderHostQuestionState();
    }
    startCountdown(payload.endsAt);
  }

  function renderHostQuestionState() {
    if (!els['choice-list']) return;
    els['choice-list'].innerHTML = '';
    const stateLine = document.createElement('div');
    stateLine.className = 'host-question-state';
    stateLine.textContent = 'Players are answering';
    els['choice-list'].appendChild(stateLine);
  }

  function renderChoices(choices) {
    if (!els['choice-list']) return;
    els['choice-list'].innerHTML = '';
    ['A', 'B', 'C', 'D'].forEach(function (letter) {
      const choice = document.createElement('button');
      choice.className = 'choice choice-' + letter.toLowerCase();
      choice.type = 'button';
      choice.dataset.option = letter;

      const label = document.createElement('span');
      label.className = 'choice-key';
      label.textContent = letter;

      const text = document.createElement('span');
      text.className = 'choice-text';
      text.textContent = choices[letter] || '';

      choice.appendChild(label);
      choice.appendChild(text);

      choice.addEventListener('click', function () {
        submitAnswer(letter);
      });

      els['choice-list'].appendChild(choice);
    });
  }

  function submitAnswer(letter) {
    if (state.answerLocked || typeof state.send !== 'function') return;
    state.answerLocked = true;
    Array.from(els['choice-list'].querySelectorAll('.choice')).forEach(function (button) {
      button.disabled = true;
      if (button.dataset.option === letter) button.classList.add('selected');
    });
    setText(els['answer-feedback'], 'Answer submitted');
    state.send('/app/game/' + state.code + '/submit', {
      type: 'answer',
      uuid: messageId(),
      payload: { selectedOption: letter }
    });
  }

  function renderLeaderboard(payload) {
    showGamePanel();
    hide(els['question-panel']);
    show(els['leaderboard-panel']);
    setText(els['game-status'], payload.finalQuestion ? 'Final Scores' : 'Leaderboard');
    setText(els['question-count'], 'After question ' + payload.questionNumber + ' of ' + payload.totalQuestions);
    setText(els['timer'], payload.finalQuestion ? 'Done' : 'Next up');

    if (els['next-question-btn']) {
      els['next-question-btn'].classList.toggle('hidden', !!payload.finalQuestion);
      els['next-question-btn'].disabled = !!payload.finalQuestion;
    }
    renderLeaderboardRows(payload.leaderboard || []);
  }

  function renderLeaderboardRows(entries) {
    if (!els['leaderboard-list']) return;
    els['leaderboard-list'].innerHTML = '';
    const maxScore = entries.reduce(function (max, entry) {
      return Math.max(max, Number(entry.totalScore) || 0);
    }, 0);

    entries.forEach(function (entry) {
      const row = document.createElement('li');
      row.className = 'leaderboard-row';
      if (entry.nickname === state.nickname) row.classList.add('is-you');

      const meta = document.createElement('div');
      meta.className = 'leaderboard-meta';
      meta.innerHTML =
        '<span class="rank">#' + entry.rank + '</span>' +
        '<span class="name"></span>' +
        '<span class="score">' + entry.totalScore + ' pts</span>';
      meta.querySelector('.name').textContent = entry.nickname;

      const bar = document.createElement('div');
      bar.className = 'score-bar';
      const fill = document.createElement('span');
      fill.style.width = maxScore === 0 ? '2%' : Math.max(8, (entry.totalScore / maxScore) * 100) + '%';
      bar.appendChild(fill);

      const detail = document.createElement('div');
      detail.className = 'leaderboard-detail';
      detail.textContent = resultText(entry);

      row.appendChild(meta);
      row.appendChild(bar);
      row.appendChild(detail);
      els['leaderboard-list'].appendChild(row);
    });

    if (entries.length === 0) {
      const empty = document.createElement('li');
      empty.className = 'empty';
      empty.textContent = 'No scores yet';
      els['leaderboard-list'].appendChild(empty);
    }
  }

function renderFinalResults(payload) {
  state.finalPayload = payload;
  showGamePanel();
  hide(els['question-panel']);
  hide(els['leaderboard-panel']);
  show(els['final-panel']);
  setText(els['game-status'], 'Game Over!');
  setText(els['question-count'], 'Final results');
  setText(els['timer'], 'Done');

  if (els['next-question-btn']) els['next-question-btn'].classList.add('hidden');
  if (els['finish-game-btn']) els['finish-game-btn'].classList.add('hidden');

  // Wrap final-panel contents in dark screen
  var fp = els['final-panel'];
  fp.classList.add('final-screen');

  // Inject confetti canvas if not already there
  if (!document.getElementById('confetti-canvas')) {
  var canvas = document.createElement('canvas');
  canvas.id = 'confetti-canvas';
  fp.insertBefore(canvas, fp.firstChild);
  setTimeout(function () { startConfetti(canvas); }, 50);
}

  // Titles
  var titleEl = fp.querySelector('.final-title');
  if (!titleEl) {
    titleEl = document.createElement('div');
    titleEl.className = 'final-title';
    fp.insertBefore(titleEl, fp.querySelector('h2') || fp.firstChild);
  }
  titleEl.textContent = 'Game Over!';
  var h2 = fp.querySelector('h2');
  if (h2) h2.style.display = 'none';

  renderPodium(payload.winners || []);
  renderSummary(payload.sessionSummary || {}, payload.leaderboard || []);
}

  function exportResultsCsv() {
    if (!state.finalPayload || !state.finalPayload.leaderboard) return;
    const rows = [
      ['Rank', 'Nickname', 'Total Score', 'Correct Answers', 'Answered Questions']
    ].concat(state.finalPayload.leaderboard.map(function (entry) {
      return [
        entry.rank,
        entry.nickname,
        entry.totalScore,
        entry.correctAnswers,
        entry.answeredQuestions
      ];
    }));
    const csv = rows.map(function (row) {
      return row.map(csvCell).join(',');
    }).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'quiz-results-' + state.code + '.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }


function renderPodium(winners) {
  if (!els.podium) return;
  els.podium.innerHTML = '';
  var medals = { 1: '🥇', 2: '🥈', 3: '🥉' };
  // Order: 2nd, 1st, 3rd (Kahoot visual order)
  var ordered = [2, 1, 3].map(function (rank) {
    return winners.find(function (w) { return Number(w.rank) === rank; });
  }).filter(Boolean);

  ordered.forEach(function (winner, i) {
    var place = document.createElement('div');
    place.className = 'podium-place podium-rank-' + winner.rank;

    var medal = document.createElement('div');
    medal.className = 'podium-medal';
    medal.textContent = medals[winner.rank] || '';

    var name = document.createElement('div');
    name.className = 'podium-name';
    name.textContent = winner.username;

    var score = document.createElement('div');
    score.className = 'podium-score';
    score.textContent = Number(winner.totalScore).toLocaleString() + ' pts';

    var block = document.createElement('div');
    block.className = 'podium-block';
    block.textContent = '#' + winner.rank;

    place.appendChild(medal);
    place.appendChild(name);
    place.appendChild(score);
    place.appendChild(block);
    els.podium.appendChild(place);

    // Stagger the rise animation
    setTimeout(function () {
      place.classList.add('podium-show');
    }, 200 + (3 - winner.rank) * 180);
  });
}


function startConfetti(canvas) {
  var ctx = canvas.getContext('2d');
// NEW
function resize() { canvas.width = window.innerWidth; canvas.height = window.innerHeight; }
  resize();
  var colors = ['#f1c232','#ff4081','#00e5ff','#69f0ae','#ff6d00','#d500f9','#fff176'];
  var pieces = Array.from({ length: 110 }, function () {
    return {
      x: Math.random() * canvas.width, y: Math.random() * -canvas.height,
      w: 6 + Math.random() * 8, h: 10 + Math.random() * 6,
      color: colors[Math.floor(Math.random() * colors.length)],
      r: Math.random() * Math.PI * 2,
      vx: (Math.random() - 0.5) * 3, vy: 2 + Math.random() * 3,
      vr: (Math.random() - 0.5) * 0.2
    };
  });
  var frame = 0;
  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    pieces.forEach(function (p) {
      ctx.save(); ctx.translate(p.x, p.y); ctx.rotate(p.r);
      ctx.fillStyle = p.color; ctx.globalAlpha = 0.85;
      ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
      ctx.restore();
      p.x += p.vx; p.y += p.vy; p.r += p.vr;
      if (p.y > canvas.height + 20) { p.y = -20; p.x = Math.random() * canvas.width; }
    });
    if (++frame < 500) requestAnimationFrame(draw);
    else ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
  draw();
}


  function renderSummary(summary, leaderboard) {
    const totalPlayers = Number(summary.totalPlayers) || leaderboard.length || 0;
    const averageScore = Number(summary.averageScore) || 0;
    let text = totalPlayers + ' players completed the game. Average score: ' + averageScore + '.';
    const mine = leaderboard.find(function (entry) { return entry.nickname === state.nickname; });
    if (mine) {
      text += ' You placed #' + mine.rank + ' with ' + mine.totalScore + ' points.';
    }
    setText(els['session-summary'], text);
  }

  function renderAnswerAck(type, payload) {
    if (!els['answer-feedback']) return;
    if (payload.accepted) {
      setText(els['answer-feedback'], 'Answer locked');
      return;
    }
    setText(els['answer-feedback'], type === 'answer_rejected'
      ? 'Answer rejected: ' + (payload.reason || 'unknown')
      : 'Answer not accepted: ' + (payload.reason || 'unknown'));
  }

  function startCountdown(endsAt) {
    if (state.countdownId) clearInterval(state.countdownId);
    function tick() {
      const remainingMs = new Date(endsAt).getTime() - Date.now();
      const seconds = Math.max(0, Math.ceil(remainingMs / 1000));
      setText(els.timer, seconds + 's');
      if (seconds <= 0 && state.countdownId) {
        clearInterval(state.countdownId);
        state.countdownId = null;
      }
    }
    tick();
    state.countdownId = setInterval(tick, 250);
  }

  function resultText(entry) {
    if (entry.lastDelta > 0) return '+' + entry.lastDelta + ' last question';
    if (entry.answeredQuestions > 0 && entry.lastAnswerCorrect === false) return 'No points last question';
    return entry.correctAnswers + ' correct';
  }

  function messageId() {
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    return 'msg-' + Date.now() + '-' + Math.random().toString(16).slice(2);
  }

  function csvCell(value) {
    const text = String(value == null ? '' : value);
    return '"' + text.replace(/"/g, '""') + '"';
  }

  function setText(el, value) {
    if (el) el.textContent = value;
  }

  function show(el) {
    if (el) el.classList.remove('hidden');
  }

  function hide(el) {
    if (el) el.classList.add('hidden');
  }

  window.QuizGameView = {
    init: init,
    handleEvent: handleEvent,
    showGamePanel: showGamePanel
  };
})();
