package com.quiz.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;

@Controller
public class GameStompController {
    private static final Logger logger = LoggerFactory.getLogger(GameStompController.class);

    private final SimpMessagingTemplate messagingTemplate;

    public GameStompController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle incoming messages from clients at /app/game/{code}/submit
     * Validate the GameMessage structure and forward to /topic/game/{code}
     */
    @MessageMapping("/game/{code}/submit")
    public void handleSubmit(@DestinationVariable String code, @Valid @Payload GameMessage msg) {
        // Server-side validation passed (validated by Spring's Validator due to @Valid)
        logger.info("Received message for code {}: type={} uuid={}", code, msg.getType(), msg.getUuid());

        // Basic server-side validation for allowed types
        String t = msg.getType();
        if (!("answer".equals(t) || "join".equals(t) || "leave".equals(t) || "ping".equals(t))) {
            // Unknown message type: ignore or return an error topic
            messagingTemplate.convertAndSend("/topic/game/" + code + "/errors", Map.of("error", "invalid_type", "type", t));
            return;
        }

        // Broadcast the message to the game topic; game engine will subscribe server-side if needed
        messagingTemplate.convertAndSend("/topic/game/" + code, msg);
    }
}
