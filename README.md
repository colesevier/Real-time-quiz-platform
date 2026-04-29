# Real-Time Quiz Platform

A Kahoot-style live quiz built with **Spring Boot 3** (embedded Tomcat), **STOMP over WebSocket**, **MySQL 8**, and Thymeleaf. Hosts upload (eventually) study material, generate a quiz, and start a session with a 6-digit invite code; players join with a nickname and play in real time.

## What's implemented today

**Authentication** ([AuthController](src/main/java/com/quiz/controller/AuthController.java))
- Register / login / logout backed by `users` table, jBCrypt password hashing, `HttpSession` cookies (30-min idle timeout)

**Host / Join lobby** (this is the part that's new)
- Host clicks **Create Game** on the dashboard → a sample 3-question quiz is seeded (Manage Game UI not built yet) → a `GameSession` row is created with a unique 6-digit code → host lands on `/host/{code}`
- Player visits `/join` (or follows the invite link), enters the code + a nickname → lands on `/play/{code}` showing "Waiting for host to start..."
- Live roster sync via STOMP: joins, leaves, kicks, and host disconnect all push events to both `/topic/host/{code}` and `/topic/game/{code}`
- Host controls: **Start**, **Cancel**, **Kick** (per-player). Start button is disabled until at least one player joins (per spec edge case).
- Personal targeted events on `/user/queue/lobby` (e.g. "you were kicked", "lobby full")

**Live scoring / leaderboard**
- Starting a lobby now initializes a server-side game engine, broadcasts each question without the correct answer, accepts player answers through `/app/game/{code}/submit`, and scores first submissions only.
- Questions close on the server timer. The server calculates speed-weighted points, broadcasts `leaderboard_update` events, and exposes the same payload on `/topic/game/{code}/leaderboard`.
- When the last question closes, the session is marked `FINISHED`, a `final_results` podium payload is broadcast, and results are also published on `/topic/game/{code}/results`.

### What is *not* built yet (planned)
- Manage Game (upload material → review/edit questions). Today the dashboard's Create Game seeds a hardcoded sample quiz so the lobby is testable.
- Persistent per-player result history and CSV export. Scores are currently held in memory for the active session and emitted over STOMP.
- Pause/Resume mid-game, host-disconnect grace timer, idle-session sweep.
- Login-as-guest.

## Architecture at a glance

```
HTTP controllers (HostController, JoinController, PlayerLobbyController, AuthController)
      │
      ▼
Services (GameSessionService, InviteCodeService, LobbyRegistry interface)
      │                                              │
      ▼                                              ▼
Repositories (JdbcTemplate)                 In-memory roster (swap to Redis later)
      │
      ▼
MySQL: users, games, questions, game_sessions
```

STOMP layer ([LobbyStompController](src/main/java/com/quiz/websocket/LobbyStompController.java)) sits beside the HTTP layer. A custom [StompPrincipalHandshakeHandler](src/main/java/com/quiz/websocket/StompPrincipalHandshakeHandler.java) reads the HTTP session at the WebSocket handshake and packages identity into a `StompPrincipal` so message handlers can authorize requests without trusting client claims.

### STOMP destinations

| Destination | Direction | Sent by | Purpose |
|---|---|---|---|
| `/app/lobby/{code}/join` | client → server | player | declare presence |
| `/app/lobby/{code}/leave` | client → server | player | explicit leave |
| `/app/lobby/{code}/start` | client → server | host only | begin game |
| `/app/lobby/{code}/cancel` | client → server | host only | tear down session |
| `/app/lobby/{code}/kick` | client → server | host only | remove a player |
| `/topic/host/{code}` | server → all (host UI) | server | roster events + admin signals |
| `/topic/game/{code}` | server → all (player UI) | server | roster events + start/cancel |
| `/user/queue/lobby` | server → one | server | personal events (kicked, error) |

## Prerequisites

- **Java 21+** (Maven wrapper / install will use whatever JDK Maven finds; pom targets Java 21, JDK 21–25 all work)
- **Maven 3.9+**
- **MySQL 8**

## First-time setup

### 1. Database

Create the schema (`kahoot` database + 4 tables):

```bash
# macOS local install
/usr/local/mysql/bin/mysql -u root -p < sql/schema.sql

# or generic
mysql -u root -p < sql/schema.sql

# or Docker
docker run -d --name kahoot-mysql -e MYSQL_ROOT_PASSWORD=yourpw -p 3306:3306 mysql:8.0
sleep 15
docker exec -i kahoot-mysql mysql -uroot -pyourpw < sql/schema.sql
```

The schema is in [sql/schema.sql](sql/schema.sql) — `users`, `games`, `questions`, `game_sessions`.

### 2. Configure DB credentials

The app reads three environment variables ([application.properties](src/main/resources/application.properties)):

```bash
export QUIZ_DB_URL='jdbc:mysql://localhost:3306/kahoot?useSSL=false&serverTimezone=UTC'
export QUIZ_DB_USER=root
export QUIZ_DB_PASS=yourpw
```

### 3. Run

```bash
mvn spring-boot:run
```

App boots in ~1 second on http://localhost:8080.

## How to test the host/join flow end-to-end

You need **two browser sessions** (regular + private/incognito so cookies don't collide).

1. **Host window**: http://localhost:8080/auth/register → register → log in → click **Create Game**. You'll see a 6-digit code, an invite link, and an empty roster.
2. **Player window**: http://localhost:8080/join → enter the code from the host window + any nickname → click **Join**. The host's roster updates instantly, the player count goes to 1, and the host's **Start** button enables.
3. Click **Start** on the host (or **Cancel**, or **Kick** a player) — both windows react in real time.

## Configurable lobby settings

In [application.properties](src/main/resources/application.properties):

```
quiz.lobby.code-length=6
quiz.lobby.code-alphabet=NUMERIC          # or ALPHANUMERIC
quiz.lobby.disconnect-grace-seconds=60
quiz.lobby.late-join=false
quiz.lobby.session-idle-sweep-minutes=120
```

## Tests

```bash
mvn test
```

23 unit tests covering:
- [RandomInviteCodeServiceTest](src/test/java/com/quiz/service/RandomInviteCodeServiceTest.java) — code format, collision retry, exhaustion
- [InMemoryLobbyRegistryTest](src/test/java/com/quiz/service/InMemoryLobbyRegistryTest.java) — capacity boundary, **100-thread concurrency proof** (50-cap room → exactly 50 OKs + 50 FULLs), kick, reattach on reconnect
- [GameSessionServiceTest](src/test/java/com/quiz/service/GameSessionServiceTest.java) — host authorization, status guards, empty-lobby start rejection
- [GameEngineServiceTest](src/test/java/com/quiz/service/GameEngineServiceTest.java) — question payload safety, speed-weighted scoring, final podium

Tests use Mockito's subclass mock maker (configured at [src/test/resources/mockito-extensions/](src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker)) for compatibility with newer JDKs.

## Project layout

```
src/main/java/com/quiz/
├── Application.java              # Spring Boot bootstrap
├── config/WebSocketConfig.java   # STOMP broker + handshake handler
├── controller/                   # HTTP routes (auth, dashboard, host, join, player lobby)
├── dao/UserDAO.java              # JdbcTemplate-backed user repo
├── repository/                   # JdbcTemplate repos for Game / Question / GameSession
├── service/                      # GameSessionService, GameEngineService, InviteCodeService, LobbyRegistry
├── websocket/                    # LobbyStompController, StompPrincipal, GameStompController
├── model/                        # Plain POJOs mirroring the DB tables
├── dto/LobbyEvent.java           # Versioned envelope for STOMP broadcasts
└── util/PasswordUtil.java        # jBCrypt wrapper

src/main/resources/
├── application.properties        # DB + lobby config
├── templates/                    # Thymeleaf views
└── static/{css,js}/              # Stylesheet and lobby.js (SockJS + stomp.js client)

sql/schema.sql                    # MySQL schema
```

## Common pitfalls

- **`Request header is too large` on first load** — old cookies from another `localhost:8080` app. Either clear cookies for `localhost`, or note that [application.properties](src/main/resources/application.properties) bumps `server.max-http-request-header-size=64KB`.
- **`Access denied for user`** when running schema.sql — the `csci201` user does not exist by default; use whatever `root` password your local MySQL has.
- **App boots but `/host/start` 404s** — make sure you're logged in; the dashboard's Create Game button POSTs from an authenticated session.
- **Static assets 404** — Spring Boot serves `src/main/resources/static/` at the URL root, so reference assets as `/css/...` and `/js/...`, not `/static/css/...`.

## Stopping the app

```bash
lsof -ti :8080 | xargs kill
```
