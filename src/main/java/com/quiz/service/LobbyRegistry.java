package com.quiz.service;

import java.util.List;
import java.util.Optional;

/**
 * Live, in-memory roster of players currently connected to a lobby.
 * Decoupled from persistence so we can swap to Redis for multi-instance scaling
 * without rewriting callers.
 */
public interface LobbyRegistry {

    JoinResult join(String code, int maxPlayers, String stompSessionId, String nickname);

    /** Remove by STOMP session id (e.g. on disconnect). Returns the dropped player if any. */
    Optional<Player> leaveBySession(String code, String stompSessionId);

    /** Remove by nickname (host kick). Returns the dropped player if any. */
    Optional<Player> kickByNickname(String code, String nickname);

    List<Player> roster(String code);

    void disposeLobby(String code);
}
