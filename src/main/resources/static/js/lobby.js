(function () {
  const code = window.LOBBY_CODE;
  const role = window.LOBBY_ROLE; // 'host' or 'player'
  const nickname = window.LOBBY_NICKNAME || '';

  const rosterEl = document.getElementById('roster');
  const playerCountEl = document.getElementById('player-count');
  const statusEl = document.getElementById('status');
  const startBtn = document.getElementById('start-btn');
  const logEl = document.getElementById('log');

  function log(msg) {
    if (!logEl) return;
    const line = document.createElement('div');
    line.textContent = '[' + new Date().toLocaleTimeString() + '] ' + msg;
    logEl.appendChild(line);
    while (logEl.childNodes.length > 30) logEl.removeChild(logEl.firstChild);
  }

  function renderRoster(names) {
    if (!rosterEl) return;
    rosterEl.innerHTML = '';
    if (!names || names.length === 0) {
      const li = document.createElement('li');
      li.className = 'empty';
      li.textContent = role === 'host' ? 'Waiting for players...' : 'No one yet';
      rosterEl.appendChild(li);
    } else {
      names.forEach(function (n) {
        const li = document.createElement('li');
        li.textContent = n;
        if (role === 'host') {
          const kick = document.createElement('button');
          kick.type = 'button';
          kick.className = 'btn btn-mini';
          kick.textContent = 'Kick';
          kick.addEventListener('click', function () { sendKick(n); });
          li.appendChild(kick);
        }
        rosterEl.appendChild(li);
      });
    }
    if (playerCountEl) playerCountEl.textContent = (names || []).length;
    if (startBtn) startBtn.disabled = !names || names.length === 0;
  }

  const sock = new SockJS('/ws');
  const stomp = Stomp.over(sock);
  stomp.debug = null; // silence console spam

  function send(destination, body) {
    stomp.send(destination, {}, JSON.stringify(body || {}));
  }

  function sendKick(targetNick) {
    send('/app/lobby/' + code + '/kick', { nickname: targetNick });
  }

  stomp.connect({}, function () {
    if (statusEl) statusEl.textContent = role === 'host' ? 'LOBBY' : 'in lobby';
    log('connected');

    const topic = role === 'host' ? '/topic/host/' + code : '/topic/game/' + code;
    stomp.subscribe(topic, function (frame) {
      const ev = JSON.parse(frame.body);
      handleLobbyEvent(ev);
    });

    stomp.subscribe('/user/queue/lobby', function (frame) {
      const ev = JSON.parse(frame.body);
      handlePersonalEvent(ev);
    });

    if (role === 'player') {
      send('/app/lobby/' + code + '/join', {});
    }
  }, function (err) {
    if (statusEl) statusEl.textContent = 'disconnected';
    log('connection error: ' + (err && err.toString ? err.toString() : err));
  });

  if (startBtn) {
    startBtn.addEventListener('click', function () {
      send('/app/lobby/' + code + '/start', {});
    });
  }

  window.addEventListener('beforeunload', function () {
    try { send('/app/lobby/' + code + '/leave', {}); } catch (e) {}
  });

  function handleLobbyEvent(ev) {
    switch (ev.type) {
      case 'player_joined':
      case 'player_left':
        renderRoster(ev.payload && ev.payload.roster);
        log(ev.type + ': ' + ((ev.payload && ev.payload.nickname) || ''));
        break;
      case 'started':
        if (statusEl) statusEl.textContent = 'ACTIVE';
        log('game started');
        // Question loop UI is the next feature; for now just show a notice.
        alert('Game started! (question loop UI not built yet)');
        break;
      case 'cancelled':
        if (statusEl) statusEl.textContent = 'cancelled';
        log('game cancelled by host');
        alert('The host cancelled the game.');
        window.location.href = '/dashboard';
        break;
      default:
        log('event: ' + ev.type);
    }
  }

  function handlePersonalEvent(ev) {
    if (ev.type === 'kicked') {
      alert('You were removed from the lobby.');
      window.location.href = '/dashboard';
    } else if (ev.type === 'error') {
      const reason = ev.payload && ev.payload.reason;
      log('error: ' + reason);
      if (reason === 'nickname_taken') alert('That nickname is already taken in this lobby.');
      else if (reason === 'lobby_full') { alert('Lobby is full.'); window.location.href = '/dashboard'; }
      else if (reason === 'not_in_lobby') { alert('Game is no longer accepting players.'); window.location.href = '/dashboard'; }
      else if (reason === 'invalid_code') { alert('Invalid code.'); window.location.href = '/dashboard'; }
    }
  }
})();
