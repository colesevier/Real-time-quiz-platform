package com.quiz.controller;

import com.quiz.model.Game;
import com.quiz.model.GameSession;
import com.quiz.model.Question;
import com.quiz.repository.GameRepository;
import com.quiz.repository.GameSessionRepository;
import com.quiz.repository.QuestionRepository;
import com.quiz.service.GameSessionService;
import com.quiz.service.SessionExceptions;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/host")
public class HostController {

    private final GameRepository games;
    private final QuestionRepository questions;
    private final GameSessionRepository sessions;
    private final GameSessionService sessionService;

    public HostController(GameRepository games, QuestionRepository questions,
                          GameSessionRepository sessions, GameSessionService sessionService) {
        this.games = games;
        this.questions = questions;
        this.sessions = sessions;
        this.sessionService = sessionService;
    }

    /**
     * Host clicks "Create Game" on the dashboard.
     * If they don't pass a gameId we seed a sample quiz so the flow is testable end-to-end
     * before the Manage-Game feature lands.
     */
    @PostMapping("/start")
    public String start(@RequestParam(required = false) Integer gameId,
                        @RequestParam(defaultValue = "20") int timePerQuestion,
                        @RequestParam(defaultValue = "50") int maxPlayers,
                        HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userID");
        if (userId == null) return "redirect:/auth/login";
        int gid = gameId != null ? gameId : seedSampleGame(userId);
        GameSession s = sessionService.createSession(gid, userId, timePerQuestion, maxPlayers);
        return "redirect:/host/" + s.getInviteCode();
    }

    @GetMapping("/{code}")
    public String hostLobby(@PathVariable String code, HttpSession session, Model model) {
        UUID userId = (UUID) session.getAttribute("userID");
        if (userId == null) return "redirect:/auth/login";
        Optional<GameSession> s = sessions.findByCode(code);
        if (s.isEmpty()) return "redirect:/dashboard";
        Game g = games.findById(s.get().getGameID()).orElse(null);
        if (g == null || !g.getUserID().equals(userId)) return "redirect:/dashboard";
        model.addAttribute("code", code);
        model.addAttribute("gameTitle", g.getGameTitle());
        model.addAttribute("status", s.get().getStatus());
        model.addAttribute("maxPlayers", s.get().getMaxPlayers());
        return "host_lobby";
    }

    @PostMapping("/{code}/cancel")
    public String cancel(@PathVariable String code, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userID");
        if (userId == null) return "redirect:/auth/login";
        try {
            sessionService.cancelSession(code, userId);
        } catch (SessionExceptions.HostMismatchException | SessionExceptions.SessionNotFoundException e) {
            // swallow — we're heading back to dashboard either way
        }
        return "redirect:/dashboard";
    }

    private int seedSampleGame(UUID userId) {
        int id = games.insert(userId, "Sample Quiz");
        List<Question> sample = List.of(
                new Question(id, "What is 2 + 2?", 'B', "3", "4", "5", "6"),
                new Question(id, "Capital of France?", 'C', "London", "Berlin", "Paris", "Madrid"),
                new Question(id, "STOMP runs over which transport here?", 'A', "WebSocket", "HTTP/2", "gRPC", "AMQP")
        );
        sample.forEach(questions::insert);
        return id;
    }

    @PostMapping("/start/{gameId}")
public String startGame(@PathVariable int gameId,
                        @RequestParam(defaultValue = "20") int timePerQuestion,
                        @RequestParam(defaultValue = "50") int maxPlayers,
                        HttpSession session) {
    UUID userId = (UUID) session.getAttribute("userID");
    if (userId == null) return "redirect:/auth/login";
    GameSession s = sessionService.createSession(gameId, userId, timePerQuestion, maxPlayers);
    return "redirect:/host/" + s.getInviteCode();
}
}
