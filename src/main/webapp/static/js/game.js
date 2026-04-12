// Basic client-side placeholder for receiving question payloads via WebSocket
(() => {
  console.log('game.js loaded');
  // Connect to websocket using same origin
  const path = window.location.pathname;
  // Expect ?code= in querystring
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code') || 'demo';
  const ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + location.pathname.replace(/\/[^/]*$/, '') + '/ws/game/' + code);

  ws.onmessage = (ev) => {
    try {
      const msg = JSON.parse(ev.data);
      // handle types: question, leaderboard, result
      if (msg.type === 'question') {
        document.getElementById('question').textContent = msg.payload.text;
      }
    } catch (e) { console.warn(e); }
  };

  document.querySelectorAll('.opt').forEach(btn => {
    btn.addEventListener('click', () => {
      const choice = btn.getAttribute('data-option');
      ws.send(JSON.stringify({type: 'answer', payload: {choice}}));
      document.getElementById('question').textContent = 'Waiting for other players...';
    });
  });
})();
