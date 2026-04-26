package com.quiz.service;

import com.quiz.dto.LobbyEvent;
import com.quiz.model.Game;
import com.quiz.model.GameSession;
import com.quiz.repository.GameRepository;
import com.quiz.repository.GameSessionRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GameSessionService {

    private final GameRepository games;
    private final GameSessionRepository sessions;
    private final InviteCodeService codes;
    private final LobbyRegistry registry;
    private final SimpMessagingTemplate broker;

    public GameSessionService(GameRepository games,
                              GameSessionRepository sessions,
                              InviteCodeService codes,
                              LobbyRegistry registry,
                              SimpMessagingTemplate broker) {
        this.games = games;
        this.sessions = sessions;
        this.codes = codes;
        this.registry = registry;
        this.broker = broker;
    }

    /** Host creates a session for a game they own. Returns the persisted session row. */
    public GameSession createSession(int gameId, UUID hostUserId, int timePerQuestion, int maxPlayers) {
        Game game = games.findById(gameId)
                .orElseThrow(() -> new SessionExceptions.SessionNotFoundException("game " + gameId));
        if (!game.getUserID().equals(hostUserId)) throw new SessionExceptions.HostMismatchException();
        String code = codes.generate();
        sessions.insert(gameId, code, timePerQuestion, maxPlayers);
        return sessions.findByCode(code).orElseThrow();
    }

    public void startSession(String code, UUID hostUserId) {
        GameSession s = requireOwnedSession(code, hostUserId);
        if (!"LOBBY".equals(s.getStatus())) {
            throw new SessionExceptions.IllegalSessionStateException("session is " + s.getStatus());
        }
        if (registry.roster(code).isEmpty()) throw new SessionExceptions.EmptyLobbyException();
        sessions.updateStatus(code, "ACTIVE");
        Map<String, Object> payload = Map.of("code", code);
        broker.convertAndSend("/topic/host/" + code, new LobbyEvent("started", payload));
        broker.convertAndSend("/topic/game/" + code, new LobbyEvent("started", payload));
    }

    public void cancelSession(String code, UUID hostUserId) {
        GameSession s = requireOwnedSession(code, hostUserId);
        if ("FINISHED".equals(s.getStatus())) return;
        sessions.updateStatus(code, "FINISHED");
        Map<String, Object> payload = Map.of("code", code, "reason", "host_cancelled");
        broker.convertAndSend("/topic/host/" + code, new LobbyEvent("cancelled", payload));
        broker.convertAndSend("/topic/game/" + code, new LobbyEvent("cancelled", payload));
        registry.disposeLobby(code);
    }

    public void kickPlayer(String code, UUID hostUserId, String nickname) {
        GameSession s = requireOwnedSession(code, hostUserId);
        if (!"LOBBY".equals(s.getStatus())) {
            throw new SessionExceptions.IllegalSessionStateException("kick only allowed in LOBBY");
        }
        registry.kickByNickname(code, nickname).ifPresent(p -> {
            broker.convertAndSendToUser(p.stompSessionId(), "/queue/lobby",
                    new LobbyEvent("kicked", Map.of("code", code)));
            broadcastRoster(code, "player_left", Map.of("nickname", p.nickname()));
        });
    }

    /** Called by the STOMP layer after a successful join in the registry. */
    public void announceJoin(String code, Player p) {
        broadcastRoster(code, "player_joined", Map.of("nickname", p.nickname()));
    }

    public void announceLeave(String code, Player p) {
        broadcastRoster(code, "player_left", Map.of("nickname", p.nickname()));
    }

    private void broadcastRoster(String code, String type, Map<String, Object> extras) {
        List<String> nicknames = registry.roster(code).stream().map(Player::nickname).toList();
        Map<String, Object> payload = new java.util.HashMap<>(extras);
        payload.put("roster", nicknames);
        LobbyEvent ev = new LobbyEvent(type, payload);
        broker.convertAndSend("/topic/host/" + code, ev);
        broker.convertAndSend("/topic/game/" + code, ev);
    }

    private GameSession requireOwnedSession(String code, UUID hostUserId) {
        GameSession s = sessions.findByCode(code)
                .orElseThrow(() -> new SessionExceptions.SessionNotFoundException(code));
        Game g = games.findById(s.getGameID())
                .orElseThrow(() -> new SessionExceptions.SessionNotFoundException("game " + s.getGameID()));
        if (hostUserId == null || !g.getUserID().equals(hostUserId)) {
            throw new SessionExceptions.HostMismatchException();
        }
        return s;
    }
}
