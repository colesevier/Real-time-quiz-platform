package com.quiz.websocket;

import com.quiz.dto.LobbyEvent;
import com.quiz.service.GameEngineService;
import com.quiz.service.SessionExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.Payload;

@Controller
public class GameStompController {
    private static final Logger logger = LoggerFactory.getLogger(GameStompController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final GameEngineService gameEngine;

    public GameStompController(SimpMessagingTemplate messagingTemplate, GameEngineService gameEngine) {
        this.messagingTemplate = messagingTemplate;
        this.gameEngine = gameEngine;
    }

    /**
     * Handle incoming messages from clients at /app/game/{code}/submit
     * Validate the GameMessage structure and let the server-side game engine score answers.
     */
    @MessageMapping("/game/{code}/submit")
    public void handleSubmit(@DestinationVariable String code,
                             @Valid @Payload GameMessage msg,
                             SimpMessageHeaderAccessor headers) {
        logger.info("Received message for code {}: type={} uuid={}", code, msg.getType(), msg.getUuid());

        String t = msg.getType();
        if ("answer".equals(t)) {
            StompPrincipal p = principal(headers);
            if (p == null || !p.isPlayer() || !code.equals(p.getJoinedCode())) {
                sendGameError(p, "not_player");
                return;
            }
            String selectedOption = selectedOption(msg.getPayload());
            if (selectedOption == null) {
                sendGameError(p, "invalid_answer");
                return;
            }
            gameEngine.submitAnswer(code, p.getNickname(), p.getName(), selectedOption);
            return;
        }

        if ("ping".equals(t)) {
            StompPrincipal p = principal(headers);
            if (p != null) {
                messagingTemplate.convertAndSendToUser(p.getName(), "/queue/game",
                        new LobbyEvent("pong", Map.of("code", code)));
            }
            return;
        }

        messagingTemplate.convertAndSend("/topic/game/" + code + "/errors",
                Map.of("error", "invalid_type", "type", t));
    }

    @MessageMapping("/game/{code}/next")
    public void nextQuestion(@DestinationVariable String code, SimpMessageHeaderAccessor headers) {
        StompPrincipal p = principal(headers);
        if (p == null || !p.isAuthenticatedUser()) {
            sendGameError(p, "not_host");
            return;
        }
        try {
            gameEngine.startNextQuestion(code, p.getUserId());
        } catch (SessionExceptions.HostMismatchException e) {
            sendGameError(p, "not_host");
        } catch (SessionExceptions.IllegalSessionStateException e) {
            sendGameError(p, "bad_state");
        } catch (SessionExceptions.SessionNotFoundException e) {
            sendGameError(p, "invalid_code");
        }
    }

    @MessageMapping("/game/{code}/finish")
    public void finishGame(@DestinationVariable String code, SimpMessageHeaderAccessor headers) {
        StompPrincipal p = principal(headers);
        if (p == null || !p.isAuthenticatedUser()) {
            sendGameError(p, "not_host");
            return;
        }
        try {
            gameEngine.finishGame(code, p.getUserId());
        } catch (SessionExceptions.HostMismatchException e) {
            sendGameError(p, "not_host");
        } catch (SessionExceptions.IllegalSessionStateException e) {
            sendGameError(p, "bad_state");
        } catch (SessionExceptions.SessionNotFoundException e) {
            sendGameError(p, "invalid_code");
        }
    }

    private static StompPrincipal principal(SimpMessageHeaderAccessor headers) {
        return headers.getUser() instanceof StompPrincipal sp ? sp : null;
    }

    private static String selectedOption(Map<String, Object> payload) {
        if (payload == null) return null;
        Object value = payload.get("selectedOption");
        if (value == null) value = payload.get("answer");
        return value instanceof String s ? s : null;
    }

    private void sendGameError(StompPrincipal p, String reason) {
        if (p == null) return;
        messagingTemplate.convertAndSendToUser(p.getName(), "/queue/game",
                new LobbyEvent("error", Map.of("reason", reason)));
    }
}
