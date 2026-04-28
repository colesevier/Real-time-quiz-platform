package com.quiz.service;

public record JoinResult(Status status, Player player) {
    public enum Status { OK, FULL, NICKNAME_TAKEN, NO_LOBBY }

    public static JoinResult ok(Player p) { return new JoinResult(Status.OK, p); }
    public static JoinResult full() { return new JoinResult(Status.FULL, null); }
    public static JoinResult nicknameTaken() { return new JoinResult(Status.NICKNAME_TAKEN, null); }
}
