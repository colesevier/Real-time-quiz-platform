package com.quiz.websocket;

import java.security.Principal;
import java.util.UUID;

/**
 * Per-WebSocket-connection identity. Captured at handshake by reading the HTTP session.
 * - Hosts have a non-null userId (set when they logged in).
 * - Players have a non-null nickname (set when they joined a code).
 * - getName() is unique per connection so /user/{name}/queue/... routing works.
 */
public final class StompPrincipal implements Principal {

    private final String name; // unique per connection
    private final UUID userId;
    private final String username;
    private final String nickname;
    private final String joinedCode;

    public StompPrincipal(UUID userId, String username, String nickname, String joinedCode) {
        this.name = "ws-" + UUID.randomUUID();
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.joinedCode = joinedCode;
    }

    @Override public String getName() { return name; }

    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getJoinedCode() { return joinedCode; }

    public boolean isAuthenticatedUser() { return userId != null; }
    public boolean isPlayer() { return nickname != null; }
}
