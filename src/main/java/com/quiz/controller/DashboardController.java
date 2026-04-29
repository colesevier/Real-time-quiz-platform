package com.quiz.controller;

import com.quiz.model.Game;
import com.quiz.repository.GameRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

@Controller
public class DashboardController {

    private final GameRepository games;

    public DashboardController(GameRepository games) {
        this.games = games;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute("userID") == null) {
            return "redirect:/auth/login";
        }
        UUID userId = (UUID) session.getAttribute("userID");
        List<Game> userGames = games.findByUser(userId);
        model.addAttribute("games", userGames);
        model.addAttribute("gameCount", userGames.size());
        return "dashboard";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}