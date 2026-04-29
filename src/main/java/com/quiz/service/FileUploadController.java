package com.quiz.controller;

import com.quiz.model.Game;
import com.quiz.model.Question;
import com.quiz.repository.GameRepository;
import com.quiz.repository.QuestionRepository;
import com.quiz.service.FileTextExtractorService;
import com.quiz.service.OllamaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final FileTextExtractorService extractor;
    private final OllamaService ollama;
    private final GameRepository games;
    private final QuestionRepository questions;

    public FileUploadController(FileTextExtractorService extractor,
                                OllamaService ollama,
                                GameRepository games,
                                QuestionRepository questions) {
        this.extractor = extractor;
        this.ollama = ollama;
        this.games = games;
        this.questions = questions;
    }


    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam("title") String title,
                                    @RequestParam(value = "count", defaultValue = "5") int count,
                                    HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userID");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "not_logged_in"));
        }

        try {
            // 1. Extract text from file
            String text = extractor.extract(file);
            if (text.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "empty_file"));
            }

            // 2. Create the game record
            int gameId = games.insert(userId, title);

            // 3. Generate questions via Ollama
            List<Question> generated = ollama.generateQuestions(gameId, text, count);
            if (generated.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "no_questions_generated"));
            }

            // 4. Save questions to DB
            for (Question q : generated) {
                questions.insert(q);
            }

            return ResponseEntity.ok(Map.of(
                    "gameId", gameId,
                    "title", title,
                    "questionsGenerated", generated.size()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "generation_failed", "detail", e.getMessage()));
        }
    }
}