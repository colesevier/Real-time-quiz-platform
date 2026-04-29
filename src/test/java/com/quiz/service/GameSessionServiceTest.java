package com.quiz.service;

import com.quiz.dto.LobbyEvent;
import com.quiz.model.Game;
import com.quiz.model.GameSession;
import com.quiz.repository.GameRepository;
import com.quiz.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameSessionServiceTest {

    private GameRepository games;
    private GameSessionRepository sessions;
    private InviteCodeService codes;
    private LobbyRegistry registry;
    private SimpMessagingTemplate broker;
    private GameEngineService gameEngine;
    private GameSessionService svc;

    private final UUID hostId = UUID.randomUUID();
    private final UUID strangerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        games = mock(GameRepository.class);
        sessions = mock(GameSessionRepository.class);
        codes = mock(InviteCodeService.class);
        registry = mock(LobbyRegistry.class);
        broker = mock(SimpMessagingTemplate.class);
        gameEngine = mock(GameEngineService.class);
        svc = new GameSessionService(games, sessions, codes, registry, broker, gameEngine);
    }

    @Test
    void createSession_persists_and_returns_row() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        when(games.findById(42)).thenReturn(Optional.of(g));
        when(codes.generate()).thenReturn("123456");
        GameSession persisted = sessionRow(42, "123456", "LOBBY");
        when(sessions.findByCode("123456")).thenReturn(Optional.of(persisted));

        GameSession out = svc.createSession(42, hostId, 20, 50);

        assertEquals("123456", out.getInviteCode());
        verify(sessions).insert(42, "123456", 20, 50);
    }

    @Test
    void createSession_rejects_when_caller_isnt_owner() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        when(games.findById(42)).thenReturn(Optional.of(g));
        assertThrows(SessionExceptions.HostMismatchException.class,
                () -> svc.createSession(42, strangerId, 20, 50));
        verify(sessions, never()).insert(eq(42), any(), eq(20), eq(50));
    }

    @Test
    void startSession_rejects_when_lobby_empty() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        GameSession s = sessionRow(42, "999000", "LOBBY");
        when(sessions.findByCode("999000")).thenReturn(Optional.of(s));
        when(games.findById(42)).thenReturn(Optional.of(g));
        when(registry.roster("999000")).thenReturn(java.util.List.of());

        assertThrows(SessionExceptions.EmptyLobbyException.class,
                () -> svc.startSession("999000", hostId));
        verify(sessions, never()).updateStatus(any(), any());
        verify(broker, never()).convertAndSend(anyString(), any(LobbyEvent.class));
        verify(gameEngine, never()).startGame(any(), any());
    }

    @Test
    void startSession_rejects_when_status_not_lobby() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        GameSession s = sessionRow(42, "999000", "ACTIVE");
        when(sessions.findByCode("999000")).thenReturn(Optional.of(s));
        when(games.findById(42)).thenReturn(Optional.of(g));

        assertThrows(SessionExceptions.IllegalSessionStateException.class,
                () -> svc.startSession("999000", hostId));
    }

    @Test
    void startSession_rejects_when_caller_isnt_host() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        GameSession s = sessionRow(42, "999000", "LOBBY");
        when(sessions.findByCode("999000")).thenReturn(Optional.of(s));
        when(games.findById(42)).thenReturn(Optional.of(g));

        assertThrows(SessionExceptions.HostMismatchException.class,
                () -> svc.startSession("999000", strangerId));
    }

    @Test
    void startSession_happy_path_updates_status_and_broadcasts() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        GameSession s = sessionRow(42, "999000", "LOBBY");
        when(sessions.findByCode("999000")).thenReturn(Optional.of(s));
        when(games.findById(42)).thenReturn(Optional.of(g));
        when(registry.roster("999000")).thenReturn(java.util.List.of(
                new Player("sid-1", "alice", Instant.now())));

        svc.startSession("999000", hostId);

        verify(sessions).updateStatus("999000", "ACTIVE");
        verify(broker).convertAndSend(eq("/topic/host/999000"), any(LobbyEvent.class));
        verify(broker).convertAndSend(eq("/topic/game/999000"), any(LobbyEvent.class));
        verify(gameEngine).startGame("999000", hostId);
    }

    @Test
    void cancelSession_marks_finished_and_disposes_lobby() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        GameSession s = sessionRow(42, "999000", "LOBBY");
        when(sessions.findByCode("999000")).thenReturn(Optional.of(s));
        when(games.findById(42)).thenReturn(Optional.of(g));
        when(registry.roster("999000")).thenReturn(java.util.List.of());

        svc.cancelSession("999000", hostId);

        verify(sessions).updateStatus("999000", "FINISHED");
        verify(gameEngine).discardGame("999000");
        verify(registry).disposeLobby("999000");
        verify(broker, times(2)).convertAndSend(anyString(), any(LobbyEvent.class));
    }

    @Test
    void kickPlayer_rejects_when_status_not_lobby() {
        Game g = new Game(42, hostId, "Quiz", Instant.now(), null);
        GameSession s = sessionRow(42, "999000", "ACTIVE");
        when(sessions.findByCode("999000")).thenReturn(Optional.of(s));
        when(games.findById(42)).thenReturn(Optional.of(g));

        assertThrows(SessionExceptions.IllegalSessionStateException.class,
                () -> svc.kickPlayer("999000", hostId, "alice"));
        verify(registry, never()).kickByNickname(any(), any());
    }

    private static String anyString() { return any(String.class); }

    private static GameSession sessionRow(int gameID, String code, String status) {
        GameSession s = new GameSession();
        s.setSessionID(1);
        s.setGameID(gameID);
        s.setInviteCode(code);
        s.setStatus(status);
        s.setTimePerQuestion(20);
        s.setMaxPlayers(50);
        return s;
    }
}
