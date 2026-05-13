# Dashboard Metrics Implementation Guide

## Current Status

The dashboard displays three key metrics for the host:

| Metric | Current Status | Data Source |
|--------|---|---|
| **Games Created** | ✅ Working | `GameRepository.findByUser(userId).size()` |
| **Total Players Hosted** | 🟡 Placeholder (returns 0) | Would need `game_players` table |
| **Last Session** | 🟡 Placeholder (returns "—") | Would use `game_sessions.startedAt` |

---

## Metric 1: Games Created ✅

**How it works:**
```
Query: SELECT * FROM games WHERE userID = ?
Implementation: GameRepository.findByUser(userId)
Template: <div class="stat-value" th:text="${gameCount}">0</div>
```

**Current Code in DashboardController:**
```java
List<Game> userGames = games.findByUser(userId);
model.addAttribute("gameCount", userGames.size());
```

**Result:** Shows count of all game definitions created by the user.

---

## Metric 2: Total Players Hosted 🟡

**How it should work:**
To count unique players across all game sessions, you need to track who participated. Currently, the database does NOT store player information.

### Option A: Store Players in Database (Recommended)

**Create a new table:**
```sql
CREATE TABLE IF NOT EXISTS game_players (
  playerID INT AUTO_INCREMENT PRIMARY KEY,
  sessionID INT NOT NULL,
  gameID INT NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  joinedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sessionID) REFERENCES game_sessions(sessionID) ON DELETE CASCADE,
  FOREIGN KEY (gameID) REFERENCES games(gameID) ON DELETE CASCADE
);
```

**Query to calculate metric:**
```sql
SELECT COUNT(DISTINCT playerID) 
FROM game_players 
WHERE gameID IN (SELECT gameID FROM games WHERE userID = ?)
```

**Implementation in DashboardController:**
```java
private int calculateTotalPlayers(List<Game> userGames) {
    if (userGames.isEmpty()) return 0;
    
    List<Integer> gameIds = userGames.stream()
        .map(Game::getGameID)
        .toList();
    
    // Would need a new method in GameSessionRepository:
    // Integer total = sessions.countTotalPlayers(gameIds);
    // return total != null ? total : 0;
    return 0; // Placeholder for now
}
```

### Option B: Estimate from Session Count

Rough estimate: count finished sessions (each session had at least 1 player):
```sql
SELECT COUNT(*) 
FROM game_sessions 
WHERE gameID IN (SELECT gameID FROM games WHERE userID = ?)
AND status = 'FINISHED'
```

**Pros:** Doesn't require schema changes
**Cons:** Inaccurate (one session ≠ one player; could be 0-50 players)

---

## Metric 3: Last Session 🟡

**How it should work:**
Query the most recent `game_session.startedAt` timestamp from user's games.

### Implementation

**Query:**
```sql
SELECT MAX(startedAt) 
FROM game_sessions 
WHERE gameID IN (SELECT gameID FROM games WHERE userID = ?)
AND status = 'FINISHED'
AND startedAt IS NOT NULL
```

**Implementation in DashboardController:**
```java
private String getLastSessionDate(List<Game> userGames) {
    if (userGames.isEmpty()) return "—";
    
    // Would need a new method in GameSessionRepository:
    // Optional<Instant> lastStarted = sessions.findMostRecentSessionTime(gameIds);
    
    // Then format it:
    // Instant instant = lastStarted.orElse(null);
    // if (instant == null) return "—";
    
    // DateTimeFormatter formatter = DateTimeFormatter
    //     .ofPattern("MMM d, h:mm a")
    //     .withZone(ZoneId.systemDefault());
    // return formatter.format(instant);
    
    return "—"; // Placeholder for now
}
```

**Example outputs:**
- "May 12, 2:30 PM"
- "Today at 3:45 PM"
- "—" (no sessions yet)

---

## How to Fully Implement

### Step 1: Add Player Tracking to Database

Update `sql/schema.sql` to add:
```sql
CREATE TABLE IF NOT EXISTS game_players (
  playerID INT AUTO_INCREMENT PRIMARY KEY,
  sessionID INT NOT NULL,
  gameID INT NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  joinedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sessionID) REFERENCES game_sessions(sessionID) ON DELETE CASCADE,
  FOREIGN KEY (gameID) REFERENCES games(gameID) ON DELETE CASCADE
);
```

### Step 2: Add Methods to GameSessionRepository

```java
public Integer countTotalPlayers(List<Integer> gameIds) {
    String ids = gameIds.stream()
        .map(String::valueOf)
        .collect(java.util.stream.Collectors.joining(","));
    
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(DISTINCT playerID) FROM game_players WHERE gameID IN (" + ids + ")",
        Integer.class);
    return count != null ? count : 0;
}

public Optional<Instant> findMostRecentSessionTime(List<Integer> gameIds) {
    String ids = gameIds.stream()
        .map(String::valueOf)
        .collect(java.util.stream.Collectors.joining(","));
    
    Timestamp ts = jdbc.queryForObject(
        "SELECT MAX(startedAt) FROM game_sessions WHERE gameID IN (" + ids + ") AND status = 'FINISHED'",
        Timestamp.class);
    
    return ts != null ? Optional.of(ts.toInstant()) : Optional.empty();
}
```

### Step 3: Update DashboardController

Replace the placeholder implementations with:

```java
private int calculateTotalPlayers(List<Game> userGames) {
    if (userGames.isEmpty()) return 0;
    List<Integer> gameIds = userGames.stream()
        .map(Game::getGameID)
        .toList();
    Integer total = sessions.countTotalPlayers(gameIds);
    return total != null ? total : 0;
}

private String getLastSessionDate(List<Game> userGames) {
    if (userGames.isEmpty()) return "—";
    List<Integer> gameIds = userGames.stream()
        .map(Game::getGameID)
        .toList();
    
    Optional<Instant> lastSession = sessions.findMostRecentSessionTime(gameIds);
    if (lastSession.isEmpty()) return "—";
    
    DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("MMM d, h:mm a")
        .withZone(ZoneId.systemDefault());
    return formatter.format(lastSession.get());
}
```

### Step 4: Track Players When They Join

In `LobbyStompController.join()`, after a player joins, record them:
```java
// After successful join to lobby:
playerRepository.insert(gameSession.getSessionID(), 
                       gameSession.getGameID(), 
                       nickname);
```

---

## Current Placeholder Code

The DashboardController currently has placeholder methods that return 0 and "—":

```java
private int calculateTotalPlayers(List<Game> userGames) {
    // TODO: Implement by querying game_players table
    return 0;
}

private String getLastSessionDate(List<Game> userGames) {
    // TODO: Implement by querying MAX(startedAt) from game_sessions
    return "—";
}
```

These are connected to the template via:
```html
<div class="stat-value" th:text="${totalPlayersHosted}">0</div>
<div class="stat-value" th:text="${lastSessionDate}">—</div>
```

---

## Summary

| Metric | Implemented | Effort | Dependencies |
|--------|---|---|---|
| Games Created | ✅ | Done | None |
| Total Players | 🟡 | Medium | Add `game_players` table, update 3 files |
| Last Session | 🟡 | Medium | Query `game_sessions`, update 2 files |

To enable "Total Players" and "Last Session", you need to:
1. Add database table(s) to track players and sessions
2. Add repository methods to query the data
3. Replace placeholder implementations in DashboardController
4. Optionally add UI to format dates nicely

All the groundwork is in place—just need to implement the database layer!
