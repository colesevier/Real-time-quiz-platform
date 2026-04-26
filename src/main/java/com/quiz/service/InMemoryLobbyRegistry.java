package com.quiz.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InMemoryLobbyRegistry implements LobbyRegistry {

    static final class Lobby {
        final ReentrantLock lock = new ReentrantLock();
        final Map<String, Player> bySid = new HashMap<>();
        final Map<String, String> nicknameToSid = new HashMap<>(); // lower-case nickname -> sid
    }

    private final ConcurrentHashMap<String, Lobby> lobbies = new ConcurrentHashMap<>();

    @Override
    public JoinResult join(String code, int maxPlayers, String sid, String nickname) {
        if (nickname == null || nickname.isBlank()) return JoinResult.nicknameTaken();
        Lobby lobby = lobbies.computeIfAbsent(code, c -> new Lobby());
        lobby.lock.lock();
        try {
            String key = nickname.toLowerCase(Locale.ROOT);
            String existingSidForNick = lobby.nicknameToSid.get(key);
            if (existingSidForNick != null) {
                // reattach: same nickname reconnecting on a new STOMP session
                if (existingSidForNick.equals(sid)) {
                    return JoinResult.ok(lobby.bySid.get(sid));
                }
                Player old = lobby.bySid.remove(existingSidForNick);
                if (old == null) {
                    // no-op: fall through to fresh insert
                } else {
                    Player swapped = new Player(sid, old.nickname(), old.joinedAt());
                    lobby.bySid.put(sid, swapped);
                    lobby.nicknameToSid.put(key, sid);
                    return JoinResult.ok(swapped);
                }
            }
            if (lobby.bySid.size() >= maxPlayers) return JoinResult.full();
            Player p = new Player(sid, nickname, Instant.now());
            lobby.bySid.put(sid, p);
            lobby.nicknameToSid.put(key, sid);
            return JoinResult.ok(p);
        } finally {
            lobby.lock.unlock();
        }
    }

    @Override
    public Optional<Player> leaveBySession(String code, String sid) {
        Lobby lobby = lobbies.get(code);
        if (lobby == null) return Optional.empty();
        lobby.lock.lock();
        try {
            Player gone = lobby.bySid.remove(sid);
            if (gone != null) lobby.nicknameToSid.remove(gone.nickname().toLowerCase(Locale.ROOT));
            return Optional.ofNullable(gone);
        } finally {
            lobby.lock.unlock();
        }
    }

    @Override
    public Optional<Player> kickByNickname(String code, String nickname) {
        Lobby lobby = lobbies.get(code);
        if (lobby == null || nickname == null) return Optional.empty();
        lobby.lock.lock();
        try {
            String sid = lobby.nicknameToSid.remove(nickname.toLowerCase(Locale.ROOT));
            if (sid == null) return Optional.empty();
            return Optional.ofNullable(lobby.bySid.remove(sid));
        } finally {
            lobby.lock.unlock();
        }
    }

    @Override
    public List<Player> roster(String code) {
        Lobby lobby = lobbies.get(code);
        if (lobby == null) return List.of();
        lobby.lock.lock();
        try {
            return new ArrayList<>(lobby.bySid.values());
        } finally {
            lobby.lock.unlock();
        }
    }

    @Override
    public void disposeLobby(String code) {
        lobbies.remove(code);
    }

    /** Test/diagnostic helper. */
    int lobbyCount() { return lobbies.size(); }
}
