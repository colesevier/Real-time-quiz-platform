package com.quiz.service;

import com.quiz.dto.LobbyEvent;
import com.quiz.model.Game;
import com.quiz.model.GameSession;
import com.quiz.model.Question;
import com.quiz.repository.GameRepository;
import com.quiz.repository.GameSessionRepository;
import com.quiz.repository.QuestionRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class GameEngineService {

    private final GameRepository games;
    private final GameSessionRepository sessions;
    private final QuestionRepository questions;
    private final LobbyRegistry registry;
    private final SimpMessagingTemplate broker;
    private final Clock clock;
    private final ScheduledExecutorService timer;
    private final ConcurrentHashMap<String, GameState> states = new ConcurrentHashMap<>();

    @Autowired
    public GameEngineService(GameRepository games,
                             GameSessionRepository sessions,
                             QuestionRepository questions,
                             LobbyRegistry registry,
                             SimpMessagingTemplate broker) {
        this(games, sessions, questions, registry, broker, Clock.systemUTC(),
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "quiz-game-timer");
                    t.setDaemon(true);
                    return t;
                }));
    }

    GameEngineService(GameRepository games,
                      GameSessionRepository sessions,
                      QuestionRepository questions,
                      LobbyRegistry registry,
                      SimpMessagingTemplate broker,
                      Clock clock,
                      ScheduledExecutorService timer) {
        this.games = games;
        this.sessions = sessions;
        this.questions = questions;
        this.registry = registry;
        this.broker = broker;
        this.clock = clock;
        this.timer = timer;
    }

    public void startGame(String code, UUID hostUserId) {
        GameSession session = requireOwnedSession(code, hostUserId);
        if (!"ACTIVE".equals(session.getStatus())) {
            throw new SessionExceptions.IllegalSessionStateException("game is not ACTIVE");
        }

        List<Question> loadedQuestions = questions.findByGame(session.getGameID());
        GameState state = new GameState(session.getGameID(), session.getTimePerQuestion(), loadedQuestions);
        registry.roster(code).forEach(p -> state.scoreFor(p.nickname()));
        states.put(code, state);

        if (loadedQuestions.isEmpty()) {
            finishGame(code, state);
            return;
        }
        startNextQuestion(code, hostUserId);
    }

    public void startNextQuestion(String code, UUID hostUserId) {
        requireOwnedSession(code, hostUserId);
        GameState state = requireState(code);
        LobbyEvent event;

        state.lock.lock();
        try {
            if (state.finished) {
                throw new SessionExceptions.IllegalSessionStateException("game is already finished");
            }
            if (state.current != null && !state.current.closed) {
                throw new SessionExceptions.IllegalSessionStateException("current question is still open");
            }
            if (state.nextQuestionIndex >= state.questions.size()) {
                event = finishGameLocked(code, state);
            } else {
                event = startNextQuestionLocked(code, state);
            }
        } finally {
            state.lock.unlock();
        }

        broadcastGameEvent(code, event);
    }

    public void finishGame(String code, UUID hostUserId) {
        requireOwnedSession(code, hostUserId);
        GameState state = requireState(code);
        finishGame(code, state);
    }

    public void submitAnswer(String code, String nickname, String userDestination, String selectedOption) {
        GameState state = states.get(code);
        if (state == null) {
            sendPersonalGameEvent(userDestination, "answer_rejected", Map.of("reason", "no_active_game"));
            return;
        }

        String normalized = normalizeOption(selectedOption);
        if (normalized == null) {
            sendPersonalGameEvent(userDestination, "answer_rejected", Map.of("reason", "invalid_answer"));
            return;
        }

        Map<String, Object> ack;
        state.lock.lock();
        try {
            if (state.finished || state.current == null || state.current.closed) {
                ack = Map.of("accepted", false, "reason", "not_accepting_answers");
            } else if (state.current.answers.containsKey(nickname)) {
                ack = Map.of(
                        "accepted", false,
                        "reason", "duplicate_answer",
                        "questionNumber", state.current.index + 1);
            } else {
                state.scoreFor(nickname);
                state.current.answers.put(nickname, new SubmittedAnswer(normalized, clock.instant()));
                ack = Map.of(
                        "accepted", true,
                        "selectedOption", normalized,
                        "questionNumber", state.current.index + 1);
            }
        } finally {
            state.lock.unlock();
        }

        sendPersonalGameEvent(userDestination, "answer_received", ack);
    }

    void expireCurrentQuestion(String code) {
        GameState state = states.get(code);
        if (state == null) return;

        List<LobbyEvent> events = new ArrayList<>();
        state.lock.lock();
        try {
            LobbyEvent leaderboard = closeQuestionLocked(code, state);
            if (leaderboard != null) events.add(leaderboard);
            if (state.finished) events.add(finalResultsEvent(code, state));
        } finally {
            state.lock.unlock();
        }

        events.forEach(ev -> broadcastGameEvent(code, ev));
    }

    public List<Map<String, Object>> currentLeaderboard(String code) {
        GameState state = states.get(code);
        if (state == null) return List.of();
        state.lock.lock();
        try {
            return leaderboardSnapshot(state);
        } finally {
            state.lock.unlock();
        }
    }

    public void discardGame(String code) {
        GameState state = states.remove(code);
        if (state == null) return;
        state.lock.lock();
        try {
            state.finished = true;
            if (state.current != null && state.current.timeout != null) {
                state.current.timeout.cancel(false);
            }
        } finally {
            state.lock.unlock();
        }
    }

    @PreDestroy
    public void shutdown() {
        timer.shutdownNow();
    }

    private void finishGame(String code, GameState state) {
        List<LobbyEvent> events = new ArrayList<>();
        state.lock.lock();
        try {
            LobbyEvent leaderboard = closeQuestionLocked(code, state);
            if (leaderboard != null) events.add(leaderboard);
            if (!state.finished) {
                events.add(finishGameLocked(code, state));
            } else {
                events.add(finalResultsEvent(code, state));
            }
        } finally {
            state.lock.unlock();
        }

        events.forEach(ev -> broadcastGameEvent(code, ev));
    }

    private LobbyEvent startNextQuestionLocked(String code, GameState state) {
        Question q = state.questions.get(state.nextQuestionIndex);
        CurrentQuestion current = new CurrentQuestion(state.nextQuestionIndex, q, clock.instant(), state.timePerQuestionSeconds);
        state.current = current;
        state.nextQuestionIndex++;

        current.timeout = timer.schedule(() -> expireCurrentQuestion(code),
                Math.max(1, state.timePerQuestionSeconds), TimeUnit.SECONDS);

        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("questionNumber", current.index + 1);
        payload.put("totalQuestions", state.questions.size());
        payload.put("questionId", q.getQuestionID());
        payload.put("prompt", q.getQuestionPrompt());
        payload.put("choices", Map.of(
                "A", q.getChoiceA(),
                "B", q.getChoiceB(),
                "C", q.getChoiceC(),
                "D", q.getChoiceD()));
        payload.put("timeLimitSeconds", state.timePerQuestionSeconds);
        payload.put("startedAt", current.startedAt.toString());
        payload.put("endsAt", current.endsAt.toString());
        return new LobbyEvent("question_started", payload);
    }

    private LobbyEvent closeQuestionLocked(String code, GameState state) {
        CurrentQuestion current = state.current;
        if (current == null || current.closed) return null;
        current.closed = true;
        if (current.timeout != null) current.timeout.cancel(false);

        char correct = Character.toUpperCase(current.question.getCorrectAnswer());
        for (PlayerScore score : state.scores.values()) {
            SubmittedAnswer answer = current.answers.get(score.nickname);
            score.lastDelta = 0;
            score.lastAnswerCorrect = false;
            if (answer == null) continue;

            score.answeredQuestions++;
            boolean isCorrect = answer.selectedOption().charAt(0) == correct;
            score.lastAnswerCorrect = isCorrect;
            if (isCorrect) {
                score.correctAnswers++;
                score.lastDelta = pointsFor(current, answer.submittedAt());
                score.totalScore += score.lastDelta;
            }
        }

        boolean finalQuestion = current.index == state.questions.size() - 1;
        if (finalQuestion) {
            state.finished = true;
            sessions.updateStatus(code, "FINISHED");
            registry.disposeLobby(code);
            states.remove(code, state);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("questionNumber", current.index + 1);
        payload.put("totalQuestions", state.questions.size());
        payload.put("correctAnswer", String.valueOf(correct));
        payload.put("leaderboard", leaderboardSnapshot(state));
        payload.put("finalQuestion", finalQuestion);
        return new LobbyEvent("leaderboard_update", payload);
    }

    private LobbyEvent finishGameLocked(String code, GameState state) {
        state.finished = true;
        sessions.updateStatus(code, "FINISHED");
        registry.disposeLobby(code);
        states.remove(code, state);
        if (state.current != null && state.current.timeout != null) {
            state.current.timeout.cancel(false);
        }
        return finalResultsEvent(code, state);
    }

    private LobbyEvent finalResultsEvent(String code, GameState state) {
        List<Map<String, Object>> leaderboard = leaderboardSnapshot(state);
        List<Map<String, Object>> winners = leaderboard.stream()
                .limit(3)
                .map(entry -> Map.of(
                        "rank", entry.get("rank"),
                        "username", entry.get("nickname"),
                        "totalScore", entry.get("totalScore")))
                .toList();
        int totalPlayers = leaderboard.size();
        int averageScore = totalPlayers == 0 ? 0 : (int) Math.round(leaderboard.stream()
                .mapToInt(entry -> (Integer) entry.get("totalScore"))
                .average()
                .orElse(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("gameId", String.valueOf(state.gameId));
        payload.put("code", code);
        payload.put("winners", winners);
        payload.put("leaderboard", leaderboard);
        payload.put("sessionSummary", Map.of(
                "totalPlayers", totalPlayers,
                "averageScore", averageScore));
        return new LobbyEvent("final_results", payload);
    }

    private List<Map<String, Object>> leaderboardSnapshot(GameState state) {
        List<PlayerScore> sorted = state.scores.values().stream()
                .sorted(Comparator
                        .comparingInt(PlayerScore::totalScore).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerScore::correctAnswers).reversed())
                        .thenComparing(s -> s.nickname.toLowerCase(Locale.ROOT)))
                .toList();

        List<Map<String, Object>> out = new ArrayList<>();
        int previousScore = Integer.MIN_VALUE;
        int previousCorrect = Integer.MIN_VALUE;
        int rank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            PlayerScore score = sorted.get(i);
            if (score.totalScore != previousScore || score.correctAnswers != previousCorrect) {
                rank = i + 1;
                previousScore = score.totalScore;
                previousCorrect = score.correctAnswers;
            }
            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", rank);
            entry.put("nickname", score.nickname);
            entry.put("totalScore", score.totalScore);
            entry.put("lastDelta", score.lastDelta);
            entry.put("correctAnswers", score.correctAnswers);
            entry.put("answeredQuestions", score.answeredQuestions);
            entry.put("lastAnswerCorrect", score.lastAnswerCorrect);
            out.add(entry);
        }
        return out;
    }

    private int pointsFor(CurrentQuestion current, Instant submittedAt) {
        long elapsedMs = Math.max(0, Duration.between(current.startedAt, submittedAt).toMillis());
        long totalMs = Math.max(1, Duration.between(current.startedAt, current.endsAt).toMillis());
        double elapsedRatio = Math.min(1.0, elapsedMs / (double) totalMs);
        return (int) Math.round(1000.0 * (1.0 - elapsedRatio * 0.5));
    }

    private GameSession requireOwnedSession(String code, UUID hostUserId) {
        GameSession session = sessions.findByCode(code)
                .orElseThrow(() -> new SessionExceptions.SessionNotFoundException(code));
        Game game = games.findById(session.getGameID())
                .orElseThrow(() -> new SessionExceptions.SessionNotFoundException("game " + session.getGameID()));
        if (hostUserId == null || !game.getUserID().equals(hostUserId)) {
            throw new SessionExceptions.HostMismatchException();
        }
        return session;
    }

    private GameState requireState(String code) {
        GameState state = states.get(code);
        if (state == null) {
            throw new SessionExceptions.IllegalSessionStateException("no active game engine state");
        }
        return state;
    }

    private void broadcastGameEvent(String code, LobbyEvent event) {
        broker.convertAndSend("/topic/host/" + code, event);
        broker.convertAndSend("/topic/game/" + code, event);
        if ("leaderboard_update".equals(event.type())) {
            broker.convertAndSend("/topic/game/" + code + "/leaderboard", event);
        } else if ("final_results".equals(event.type())) {
            broker.convertAndSend("/topic/game/" + code + "/results", event);
        }
    }

    private void sendPersonalGameEvent(String userDestination, String type, Map<String, Object> payload) {
        if (userDestination == null) return;
        broker.convertAndSendToUser(userDestination, "/queue/game", new LobbyEvent(type, payload));
    }

    private static String normalizeOption(String selectedOption) {
        if (selectedOption == null) return null;
        String trimmed = selectedOption.trim().toUpperCase(Locale.ROOT);
        return trimmed.matches("[ABCD]") ? trimmed : null;
    }

    private static final class GameState {
        final ReentrantLock lock = new ReentrantLock();
        final int gameId;
        final int timePerQuestionSeconds;
        final List<Question> questions;
        final Map<String, PlayerScore> scores = new HashMap<>();
        int nextQuestionIndex = 0;
        CurrentQuestion current;
        boolean finished;

        GameState(int gameId, int timePerQuestionSeconds, List<Question> questions) {
            this.gameId = gameId;
            this.timePerQuestionSeconds = Math.max(1, timePerQuestionSeconds);
            this.questions = List.copyOf(questions);
        }

        PlayerScore scoreFor(String nickname) {
            return scores.computeIfAbsent(nickname, PlayerScore::new);
        }
    }

    private static final class CurrentQuestion {
        final int index;
        final Question question;
        final Instant startedAt;
        final Instant endsAt;
        final Map<String, SubmittedAnswer> answers = new HashMap<>();
        ScheduledFuture<?> timeout;
        boolean closed;

        CurrentQuestion(int index, Question question, Instant startedAt, int timeLimitSeconds) {
            this.index = index;
            this.question = question;
            this.startedAt = startedAt;
            this.endsAt = startedAt.plusSeconds(timeLimitSeconds);
        }
    }

    private static final class PlayerScore {
        final String nickname;
        int totalScore;
        int correctAnswers;
        int answeredQuestions;
        int lastDelta;
        boolean lastAnswerCorrect;

        PlayerScore(String nickname) {
            this.nickname = nickname;
        }

        int totalScore() {
            return totalScore;
        }

        int correctAnswers() {
            return correctAnswers;
        }
    }

    private record SubmittedAnswer(String selectedOption, Instant submittedAt) {}
}
