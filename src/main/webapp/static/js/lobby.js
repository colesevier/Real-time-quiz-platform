(() => {
  console.log('lobby.js loaded');
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code') || 'demo';
  const ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws/game/' + code);

  ws.onmessage = (ev) => {
    try {
      const msg = JSON.parse(ev.data);
      if (msg.type === 'player_join') {
        // update player list
        document.getElementById('players').textContent = 'Players (' + msg.payload.count + ')';
      }
    } catch (e) { console.warn(e); }
  };

  document.getElementById('start').addEventListener('click', () => {
    ws.send(JSON.stringify({type: 'start'}));
  });
  document.getElementById('cancel').addEventListener('click', () => {
    ws.send(JSON.stringify({type: 'cancel'}));
    window.location.href = '/dashboard';
  });
})();
