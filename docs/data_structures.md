# Data Structures Used

This document lists important data structures used in the project and why they were chosen.

- ConcurrentHashMap<String, Lobby> (`InMemoryLobbyRegistry.lobbies`): allows concurrent reads and thread-safe creation of lobby entries. The per-lobby locking avoids global contention.
- HashMap for per-lobby maps (`bySid`, `nicknameToSid`): O(1) lookup for session id and nickname mapping; protected by per-lobby `ReentrantLock` to ensure thread-safety.
- ReentrantLock per-lobby / per-game (`Lobby.lock`, `GameState.lock`): fine-grained locking to protect mutations while keeping other lobbies/games responsive.
- ScheduledExecutorService (single-threaded) for timers: centralizes timer tasks for question expiration and ensures firing order; daemon thread to avoid blocking shutdown.
- List.copyOf for question lists: immutable snapshot semantics when game starts, preventing concurrent modification surprises.
- Map and List for leaderboard calculation: simple in-memory aggregation of `PlayerScore` objects for sorting and ranking.

Rationale
- These choices prioritize low-latency in-memory operations for active game sessions and simplicity of reasoning about concurrency. Persisted state is committed to the relational DB (MySQL) at important transitions (start/finish session) to balance performance and durability.
