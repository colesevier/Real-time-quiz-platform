package com.quiz.model;

import java.time.Instant;

public class Game {
    private int gameID;
    private byte[] userID;
    private String gameTitle;
    private Instant createdAt;
    private Instant lastPlayedAt;

    public int getGameID() { return gameID; }
    public void setGameID(int gameID) { this.gameID = gameID; }

    public byte[] getUserID() { return userID; }
    public void setUserID(byte[] userID) { this.userID = userID; }

    public String getGameTitle() { return gameTitle; }
    public void setGameTitle(String gameTitle) { this.gameTitle = gameTitle; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(Instant lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }
}
