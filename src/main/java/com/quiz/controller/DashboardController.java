package com.quiz.controller;

import com.quiz.model.Game;
import com.quiz.repository.GameRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        
        // Group games by title to reduce screen clutter
        Map<String, GameGroup> grouped = new HashMap<>();
        for (Game game : userGames) {
            grouped.computeIfAbsent(game.getGameTitle(), title -> new GameGroup(title))
                   .add(game);
        }
        
        List<GameGroup> groupedGames = new ArrayList<>(grouped.values());
        model.addAttribute("groupedGames", groupedGames);
        model.addAttribute("games", userGames);
        model.addAttribute("gameCount", userGames.size());
        return "dashboard";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    /**
     * Groups games by title, with support for expanding to show all instances.
     */
    public static class GameGroup {
        private final String gameTitle;
        private final List<Game> games = new ArrayList<>();

        public GameGroup(String gameTitle) {
            this.gameTitle = gameTitle;
        }

        public void add(Game game) {
            games.add(game);
        }

        public String getGameTitle() {
            return gameTitle;
        }

        public List<Game> getGames() {
            return games;
        }

        public int getCount() {
            return games.size();
        }

        public Game getLatest() {
            return games.isEmpty() ? null : games.get(0);
        }
    }
}