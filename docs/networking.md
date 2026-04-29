# Networking & Message Protocol

This file documents network endpoints, STOMP topics, and the JSON message contract used by the app.

HTTP/REST endpoints (Thymeleaf views):
- `GET /auth/login`, `POST /auth/login` — login page and handler
- `POST /auth/guest` — guest flow (sets `guestName` in session)
- `GET /join`, `POST /join` — join a game by code and nickname

WebSocket / STOMP
- STOMP endpoint: `/ws` (SockJS fallback enabled)
- Application prefix for client messages: `/app`
- Server broker topic prefixes: `/topic`

Common topics (server -> clients):
- `/topic/host/{code}` — events for the host view (player joined/left, game control events)
- `/topic/game/{code}` — events for players (question_started, leaderboard_update, final_results)
- `/topic/game/{code}/leaderboard` — intermediate leaderboard updates
- `/topic/game/{code}/results` — final podium/results

Client destinations (client -> server):
- `/app/lobby/{code}/join` — player attempts to join lobby
- `/app/lobby/{code}/start` — host starts game
- `/app/game/{code}/submit` — player submits an answer

Message contract: `GameMessage`
```json
{
  "type": "answer|join|leave|ping",
  "uuid": "client-generated-uuid",
  "payload": { ... }
}
```
- `type` (string): message type
- `uuid` (string): id for dedup/tracing
- `payload` (object): type-specific fields

Example: answer payload
```json
{
  "type": "answer",
  "uuid": "abc-123",
  "payload": { "selectedOption": "A" }
}
```

Manual test using browser console (requires sockjs + stompjs loaded):
```javascript
const sock = new SockJS('/ws');
const client = Stomp.over(sock);
client.connect({}, function(frame) {
  client.subscribe('/topic/game/DEMO', msg => console.log('msg', msg.body));
  client.send('/app/game/DEMO/submit', {}, JSON.stringify({type:'join', uuid:'1', payload:{nickname:'G'}}));
});
```

Scaling note: the default in-memory broker is not suitable for multi-instance horizontal scaling. For production multi-instance deployments, consider RabbitMQ or Redis as a broker and configure Spring to use that broker (or use sticky sessions at the load balancer).
