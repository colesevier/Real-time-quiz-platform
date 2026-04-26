package com.quiz.service;

public final class SessionExceptions {

    private SessionExceptions() {}

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String code) { super("session not found: " + code); }
    }

    public static class HostMismatchException extends RuntimeException {
        public HostMismatchException() { super("caller is not the host of this session"); }
    }

    public static class IllegalSessionStateException extends RuntimeException {
        public IllegalSessionStateException(String msg) { super(msg); }
    }

    public static class EmptyLobbyException extends RuntimeException {
        public EmptyLobbyException() { super("cannot start: lobby is empty"); }
    }
}
