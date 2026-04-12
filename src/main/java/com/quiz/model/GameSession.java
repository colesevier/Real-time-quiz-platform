package com.quiz.model;

import java.time.Instant;

public class GameSession {
    private int sessionID;
    private int gameID;
    private String inviteCode;
    private String status; // LOBBY, ACTIVE, FINISHED
    private int timePerQuestion;
    private int maxPlayers;
    private Instant startedAt;

    public int getSessionID() { return sessionID; }
    public void setSessionID(int sessionID) { this.sessionID = sessionID; }

    public int getGameID() { return gameID; }
    public void setGameID(int gameID) { this.gameID = gameID; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTimePerQuestion() { return timePerQuestion; }
    public void setTimePerQuestion(int timePerQuestion) { this.timePerQuestion = timePerQuestion; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
}
