package com.quiz.dto;

import java.util.Map;

/**
 * Envelope for events broadcast to /topic/host/{code} and /topic/game/{code}.
 * `version` lets us evolve payloads without breaking older clients.
 */
public record LobbyEvent(String type, int version, Map<String, Object> payload) {
    public LobbyEvent(String type, Map<String, Object> payload) {
        this(type, 1, payload);
    }
}
