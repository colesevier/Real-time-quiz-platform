package com.quiz.service;

import com.quiz.dto.LobbyEvent;
import com.quiz.model.Game;
import com.quiz.model.GameSession;
import com.quiz.model.Question;
import com.quiz.repository.GameRepository;
import com.quiz.repository.GameSessionRepository;
import com.quiz.repository.QuestionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameEngineServiceTest {

    private static final String CODE = "ABC123";

    private GameRepository games;
    private GameSessionRepository sessions;
    private QuestionRepository questions;
    private LobbyRegistry registry;
    private SimpMessagingTemplate broker;
    private MutableClock clock;
    private ScheduledExecutorService timer;
    private GameEngineService svc;

    private final UUID hostId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        games = mock(GameRepository.class);
        sessions = mock(GameSessionRepository.class);
        questions = mock(QuestionRepository.class);
        registry = mock(LobbyRegistry.class);
        broker = mock(SimpMessagingTemplate.class);
        clock = new MutableClock(Instant.parse("2026-04-29T12:00:00Z"));
        timer = Executors.newSingleThreadScheduledExecutor();
        svc = new GameEngineService(games, sessions, questions, registry, broker, clock, timer);

        when(games.findById(42)).thenReturn(Optional.of(new Game(42, hostId, "Quiz", clock.instant(), null)));
        when(sessions.findByCode(CODE)).thenReturn(Optional.of(sessionRow()));
        when(questions.findByGame(42)).thenReturn(List.of(
                question(1, 'A', "Two plus two?", "4", "3", "2", "1"),
                question(2, 'B', "Capital of France?", "Rome", "Paris", "Berlin", "Madrid")
        ));
        when(registry.roster(CODE)).thenReturn(List.of(
                new Player("sid-1", "alice", clock.instant()),
                new Player("sid-2", "bob", clock.instant())
        ));
    }

    @AfterEach
    void tearDown() {
        timer.shutdownNow();
    }

    @Test
    void startGame_broadcastsQuestionWithoutCorrectAnswer() {
        svc.startGame(CODE, hostId);

        LobbyEvent ev = latestGameEvent("question_started");

        assertEquals("question_started", ev.type());
        assertEquals(1, ev.payload().get("questionNumber"));
        assertEquals(2, ev.payload().get("totalQuestions"));
        assertFalse(ev.payload().containsKey("correctAnswer"), "clients must not receive answers before scoring");
    }

    @Test
    void expireQuestion_scoresCorrectAnswersBySpeedAndRanksPlayers() {
        svc.startGame(CODE, hostId);

        clock.advance(Duration.ofSeconds(5));
        svc.submitAnswer(CODE, "alice", "user-alice", "A");
        clock.advance(Duration.ofSeconds(5));
        svc.submitAnswer(CODE, "bob", "user-bob", "B");
        svc.expireCurrentQuestion(CODE);

        LobbyEvent ev = latestGameEvent("leaderboard_update");
        List<Map<String, Object>> leaderboard = leaderboard(ev);

        assertEquals("A", ev.payload().get("correctAnswer"));
        assertEquals("alice", leaderboard.get(0).get("nickname"));
        assertEquals(875, leaderboard.get(0).get("totalScore"));
        assertEquals(0, leaderboard.get(1).get("totalScore"));
    }

    @Test
    void finalQuestion_marksSessionFinishedAndBroadcastsPodium() {
        svc.startGame(CODE, hostId);
        svc.submitAnswer(CODE, "alice", "user-alice", "A");
        svc.expireCurrentQuestion(CODE);

        svc.startNextQuestion(CODE, hostId);
        clock.advance(Duration.ofSeconds(1));
        svc.submitAnswer(CODE, "bob", "user-bob", "B");
        svc.expireCurrentQuestion(CODE);

        LobbyEvent ev = latestGameEvent("final_results");
        List<Map<String, Object>> winners = winners(ev);

        verify(sessions).updateStatus(CODE, "FINISHED");
        verify(registry).disposeLobby(CODE);
        assertEquals("alice", winners.get(0).get("username"));
        assertEquals(2, ((Map<?, ?>) ev.payload().get("sessionSummary")).get("totalPlayers"));
    }

    private LobbyEvent latestGameEvent(String type) {
        ArgumentCaptor<LobbyEvent> captor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(broker, atLeastOnce()).convertAndSend(eq("/topic/game/" + CODE), captor.capture());
        List<LobbyEvent> matches = captor.getAllValues().stream()
                .filter(ev -> type.equals(ev.type()))
                .toList();
        assertFalse(matches.isEmpty(), "expected event " + type);
        return matches.get(matches.size() - 1);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> leaderboard(LobbyEvent ev) {
        return (List<Map<String, Object>>) ev.payload().get("leaderboard");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> winners(LobbyEvent ev) {
        return (List<Map<String, Object>>) ev.payload().get("winners");
    }

    private static GameSession sessionRow() {
        GameSession s = new GameSession();
        s.setSessionID(1);
        s.setGameID(42);
        s.setInviteCode(CODE);
        s.setStatus("ACTIVE");
        s.setTimePerQuestion(20);
        s.setMaxPlayers(50);
        return s;
    }

    private static Question question(int id, char correct, String prompt,
                                     String a, String b, String c, String d) {
        Question q = new Question(42, prompt, correct, a, b, c, d);
        q.setQuestionID(id);
        return q;
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
