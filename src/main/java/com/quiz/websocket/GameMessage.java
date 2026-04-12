package com.quiz.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class GameMessage {
    @NotBlank
    private String type; // e.g., question, answer, join, start

    @NotBlank
    private String uuid; // client-generated or server-assigned message id

    @NotNull
    private Map<String, Object> payload;

    public GameMessage() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
