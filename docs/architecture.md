# Architecture Overview

This document summarizes key design choices for the Real-Time Quiz Platform, what worked, and what we would change.

Components
- Spring Boot application (controllers, services, STOMP controllers)
- STOMP over WebSocket (SockJS fallback) for real-time messages
- MySQL 8 for persistent data (games, questions, sessions, users)
- In-memory lobby registry for live player rosters

Key design choices
- Server-authoritative GameEngine (see `GameEngineService`): the server manages the current question, timers, accepts answers, and computes scores. This prevents client-side cheating.
- In-memory lobby registry (`InMemoryLobbyRegistry`): fast lookups and nickname uniqueness enforcement per lobby. Protected by per-lobby `ReentrantLock` for safe concurrent access.
- Mixed persistence: Spring Data repositories for domain objects (games/questions/sessions) and a hand-rolled `UserDAO` for user-related operations. This was an iterative decision and should be consolidated into Spring Data JPA for consistency.

What worked
- The GameEngine design with a scheduled executor and per-game locks provided predictable timing and simplified scoreboard computation.
- STOMP messaging with `SimpMessagingTemplate` simplified broadcasting leaderboards, questions, and results.

What didn't
- Mixed DAO patterns added friction. Plan: migrate `UserDAO` to a `UserRepository` JPA entity and remove `DBConnection` to use Spring Boot DataSource config.
- No DB migration tool initially; plan: add Flyway for repeatable migrations.

Future improvements
- External message broker (RabbitMQ) or Redis-backed broker for multi-instance scaling of STOMP topics.
- Add Flyway migrations and Testcontainers-based integration tests for CI.
