package com.quiz.model;

import java.time.Instant;
import java.util.UUID;

public class Game {
    private int gameID;
    private UUID userID;
    private String gameTitle;
    private Instant createdAt;
    private Instant lastPlayedAt;

    public Game() {}

    public Game(int gameID, UUID userID, String gameTitle, Instant createdAt, Instant lastPlayedAt) {
        this.gameID = gameID;
        this.userID = userID;
        this.gameTitle = gameTitle;
        this.createdAt = createdAt;
        this.lastPlayedAt = lastPlayedAt;
    }

    public int getGameID() { return gameID; }
    public void setGameID(int gameID) { this.gameID = gameID; }

    public UUID getUserID() { return userID; }
    public void setUserID(UUID userID) { this.userID = userID; }

    public String getGameTitle() { return gameTitle; }
    public void setGameTitle(String gameTitle) { this.gameTitle = gameTitle; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(Instant lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }
}
