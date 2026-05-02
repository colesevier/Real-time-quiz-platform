package com.quiz.controller;

import com.quiz.dto.ManualGameRequest;
import com.quiz.model.Question;
import com.quiz.repository.GameRepository;
import com.quiz.repository.QuestionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/manual")
public class ManualGameController {

    private final GameRepository games;
    private final QuestionRepository questions;

    public ManualGameController(GameRepository games, QuestionRepository questions) {
        this.games = games;
        this.questions = questions;
    }

    @GetMapping("/create")
    public String showForm() {
        return "create-manual";
    }

    @PostMapping("/create")
    public String processManualCreate(@ModelAttribute ManualGameRequest request, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userID");
        if (userId == null) return "redirect:/auth/login";

        // 1. Create the Game
        int gameId = games.insert(userId, request.getTitle());

        // 2. Loop through the form input and create Question objects
        for (ManualGameRequest.QuestionInput input : request.getQuestions()) {
            Question q = new Question(
                gameId,
                input.getPrompt(),
                input.getCorrect(),
                input.getA(),
                input.getB(),
                input.getC(),
                input.getD()
            );
            questions.insert(q);
        }

        return "redirect:/dashboard";
    }
}