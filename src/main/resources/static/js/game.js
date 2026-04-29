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
    setText(els['game-status'], 'Game Complete');
    setText(els['question-count'], 'Final results');
    setText(els['timer'], 'Done');

    if (els['next-question-btn']) els['next-question-btn'].classList.add('hidden');
    if (els['finish-game-btn']) els['finish-game-btn'].classList.add('hidden');

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
    const ordered = [2, 1, 3].map(function (rank) {
      return winners.find(function (winner) { return Number(winner.rank) === rank; });
    }).filter(Boolean);

    ordered.forEach(function (winner) {
      const block = document.createElement('div');
      block.className = 'podium-place podium-rank-' + winner.rank;
      block.innerHTML =
        '<div class="podium-rank">#' + winner.rank + '</div>' +
        '<div class="podium-name"></div>' +
        '<div class="podium-score">' + winner.totalScore + ' pts</div>';
      block.querySelector('.podium-name').textContent = winner.username;
      els.podium.appendChild(block);
    });
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
