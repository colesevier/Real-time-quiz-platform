package com.quiz.controller;

import com.quiz.model.Game;
import com.quiz.repository.GameRepository;
import com.quiz.repository.GameSessionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class DashboardController {

    private final GameRepository games;
    private final GameSessionRepository sessions;

    public DashboardController(GameRepository games, GameSessionRepository sessions) {
        this.games = games;
        this.sessions = sessions;
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
        
        // Calculate metrics from database
        model.addAttribute("totalPlayersHosted", calculateTotalPlayers(userGames));
        model.addAttribute("lastSessionDate", getLastSessionDate(userGames));
        
        return "dashboard";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    /**
     * Calculates total number of unique players who have participated in this user's hosted games.
     * This is a rough estimate based on the number of finished sessions.
     * For accurate player counts, you would need to store player roster in the database.
     * 
     * @param userGames list of games owned by the user
     * @return estimated total unique players
     */
    private int calculateTotalPlayers(List<Game> userGames) {
        // Note: This requires tracking players per session in the database.
        // Currently, we don't have a players table, so this is a placeholder.
        // In a real implementation, you would:
        // 1. Add a game_players table (gameID, sessionID, nickname, playerID)
        // 2. Query: SELECT COUNT(DISTINCT playerID) FROM game_players WHERE gameID IN (...)
        // 3. Return that count
        return 0;
    }

    /**
     * Formats the most recent game session timestamp for display.
     * Returns a human-readable format like "Today at 2:30 PM" or "May 12, 2:30 PM"
     * 
     * @param userGames list of games owned by the user
     * @return formatted last session date, or "—" if no sessions exist
     */
    private String getLastSessionDate(List<Game> userGames) {
        // Note: GameSession table has startedAt timestamp, but we don't currently
        // query sessions in the dashboard. To implement this:
        // 1. Query: SELECT MAX(startedAt) FROM game_sessions WHERE gameID IN (...)
        // 2. Format the timestamp for display
        // 3. Return human-readable format
        return "—";
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