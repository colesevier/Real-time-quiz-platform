# Real-Time Quiz Platform

A Kahoot-style live quiz built with **Spring Boot 3**, **STOMP over WebSocket**, **MySQL 8**, and **Thymeleaf**. Hosts create a session with a 6-digit invite code; players join with a nickname and compete in real time.

---

## Prerequisites

Install these before starting. Jump to [Installing Prerequisites](#installing-prerequisites) at the bottom if you need help.

- [Java 21](https://adoptium.net/) — the project requires exactly Java 21
- [Maven 3.9+](https://maven.apache.org/download.cgi) — for building the project
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — for running the app and database
- [Git](https://git-scm.com/) — for cloning the repository
- [Ollama](https://ollama.com/) — for AI quiz generation

---

## First-Time Setup

### 1. Clone the Repository

```bash
cd Desktop
git clone https://github.com/colesevier/Real-time-quiz-platform.git
cd Real-time-quiz-platform
git checkout develop
```

Verify you are on the correct branch:

```bash
git branch
# Should show: * develop
```

### 2. Install Ollama & Pull the Model

**macOS**
```bash
brew install ollama

# Terminal 1 — leave this running
ollama serve

# Terminal 2
ollama pull llama3.2
```

**Windows** — Download the installer from https://ollama.com/download, then:
```bash
ollama pull llama3.2
```

**Linux**
```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama serve &
ollama pull llama3.2
```

### 3. Create the `.env` File

This file holds your database password locally and is never pushed to GitHub (already in `.gitignore`).

```bash
echo "QUIZ_DB_PASS=CS201_group_4_password" > .env
```

> **Windows PowerShell:** use `Set-Content -Encoding utf8 .env "QUIZ_DB_PASS=CS201_group_4_password"` to avoid encoding issues.

### 4. Build the JAR

Make sure Docker Desktop is open, then run:

```bash
mvn -DskipTests package
```

You should see `BUILD SUCCESS` at the end.

### 5. Start the App

```bash
docker compose up -d --build
```

Verify both containers are running:

```bash
docker compose ps
```

Both `kahoot-mysql` and `kahoot-app` should show status `Up`. The database and schema are created automatically — no manual setup needed.

### 6. Open in Browser

```
http://localhost:8080/auth/register
```

Register an account and start using the app.

---

## Daily Workflow

**Starting the app:**
1. Open Docker Desktop and wait for it to finish loading
2. In the project folder, run: `docker compose up -d`

**Stopping the app:**
```bash
docker compose down
```

**After pulling new code or changing Java files:**
```bash
git pull
docker compose down
mvn -DskipTests package
docker compose up -d --build
```

---

## Testing the Host/Join Flow

You need two browser sessions (one regular + one private/incognito so cookies don't collide).

1. **Host window** — go to `http://localhost:8080/auth/register` → register → log in → click **Create Game**. You'll see a 6-digit code, an invite link, and an empty roster.
2. **Player window** — go to `http://localhost:8080/join` → enter the code + any nickname → click **Join**. The host's roster updates instantly and the Start button enables.
3. Click **Start** on the host (or Cancel, or Kick a player) — both windows react in real time.

---

## What's Implemented

**Authentication** — register, login, logout with bcrypt password hashing and session cookies.

**Host/Join Lobby** — host creates a game session with a unique 6-digit code; players join via `/join` or an invite link. Live roster sync via STOMP (joins, leaves, kicks, host disconnect).

**Live Scoring** — server broadcasts questions without answers, accepts player submissions, calculates speed-weighted points, and broadcasts leaderboard updates. Final results are broadcast when the last question closes.

---

## What's Not Built Yet

- Manage Game (upload study material → generate/edit questions). For now, Create Game seeds a hardcoded 3-question sample quiz.
- Persistent per-player result history and CSV export.
- Pause/Resume mid-game, host-disconnect grace timer, idle-session sweep.
- Login as guest.

---

## Troubleshooting

**Port 8080 already in use**
```bash
# macOS/Linux
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**App container keeps restarting**
```bash
docker compose logs app
```
Look for errors near the bottom of the output.

**Pages returning 404** — Every URL needs both a controller method in `src/main/java/com/quiz/controller/` and a matching `.html` file in `src/main/resources/templates/`.

**Database not initialized** — Verify the schema was imported:
```bash
docker exec -it kahoot-mysql mysql -u quiz_user -pCS201_group_4_password kahoot -e "SHOW TABLES;"
```
You should see: `users`, `games`, `questions`, `game_sessions`. If tables are missing, bring everything down with a clean volume:
```bash
docker compose down -v
docker compose up -d --build
```

**Static assets returning 404** — Spring Boot serves `src/main/resources/static/` at the URL root. Reference assets as `/css/...` and `/js/...`, not `/static/css/...`.

**`.env` file encoding error on Windows** — the `echo` command creates a UTF-16 file. Use `Set-Content -Encoding utf8` instead (see Step 3).

**Request header too large on first load** — old cookies from another `localhost:8080` app. Clear cookies for localhost in your browser.

**App boots but `/host/start` returns 404** — make sure you're logged in before clicking Create Game.

---

## Running Tests

```bash
mvn test
```

23 unit tests covering invite code generation, lobby concurrency, game session guards, and scoring logic.

---

## Installing Prerequisites

### macOS

Open a new terminal and run:

```bash
# 1. Install Homebrew (if you don't have it)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Install Java 21
brew install --cask temurin@21
java -version  # Should print: openjdk 21

# 3. Install Maven
brew install maven
mvn -version  # Should print: Apache Maven 3.x

# 4. Install Git
brew install git

# 5. Install Docker Desktop
# Download from https://www.docker.com/products/docker-desktop/
# Open Docker Desktop and wait for it to finish starting before continuing.
```

### Windows

Open a new terminal (Command Prompt or PowerShell) and follow these steps:

1. **Java 21** — Download and run the installer from https://adoptium.net/ — select Java 21 (LTS). Verify: `java -version`
2. **Maven** — Download from https://maven.apache.org/download.cgi, extract it, and add the `bin` folder to your system PATH. Verify: `mvn -version`
3. **Git** — Download from https://git-scm.com/ and run the installer.
4. **Docker Desktop** — Download from https://www.docker.com/products/docker-desktop/ and run the installer. Open Docker Desktop and wait for it to finish starting before continuing.

---

## Project Structure

```
src/main/java/com/quiz/
├── Application.java
├── config/WebSocketConfig.java
├── controller/          # HTTP routes (auth, dashboard, host, join, player lobby)
├── dao/UserDAO.java
├── repository/          # JdbcTemplate repos for Game / Question / GameSession
├── service/             # GameSessionService, GameEngineService, InviteCodeService, LobbyRegistry
├── websocket/           # LobbyStompController, StompPrincipal, GameStompController
├── model/               # POJOs mirroring DB tables
├── dto/LobbyEvent.java
└── util/PasswordUtil.java

src/main/resources/
├── application.properties
├── templates/           # Thymeleaf views
└── static/{css,js}/     # Stylesheets and lobby.js (SockJS + stomp.js)

sql/schema.sql           # MySQL schema
```
