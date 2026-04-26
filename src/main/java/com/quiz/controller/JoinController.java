package com.quiz.controller;

import com.quiz.model.GameSession;
import com.quiz.repository.GameSessionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class JoinController {

    private final GameSessionRepository sessions;

    public JoinController(GameSessionRepository sessions) {
        this.sessions = sessions;
    }

    @GetMapping("/join")
    public String form(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("code", code == null ? "" : code);
        return "join";
    }

    @PostMapping("/join")
    public String submit(@RequestParam String code,
                         @RequestParam String nickname,
                         HttpSession session,
                         Model model) {
        String trimmedCode = code == null ? "" : code.trim();
        String trimmedNick = nickname == null ? "" : nickname.trim();
        if (trimmedCode.isEmpty() || trimmedNick.isEmpty()) {
            model.addAttribute("code", trimmedCode);
            model.addAttribute("error", "Code and nickname are required.");
            return "join";
        }
        if (trimmedNick.length() > 24) {
            model.addAttribute("code", trimmedCode);
            model.addAttribute("error", "Nickname must be 24 characters or fewer.");
            return "join";
        }
        Optional<GameSession> s = sessions.findByCode(trimmedCode);
        if (s.isEmpty()) {
            model.addAttribute("code", trimmedCode);
            model.addAttribute("error", "Invalid code");
            return "join";
        }
        if (!"LOBBY".equals(s.get().getStatus())) {
            model.addAttribute("code", trimmedCode);
            model.addAttribute("error", "Session is no longer accepting players.");
            return "join";
        }
        session.setAttribute("nickname", trimmedNick);
        session.setAttribute("playerCode", trimmedCode);
        return "redirect:/play/" + trimmedCode;
    }
}
