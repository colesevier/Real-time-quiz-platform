package com.quiz.websocket;

import com.quiz.dto.LobbyEvent;
import com.quiz.model.GameSession;
import com.quiz.repository.GameSessionRepository;
import com.quiz.service.GameSessionService;
import com.quiz.service.JoinResult;
import com.quiz.service.LobbyRegistry;
import com.quiz.service.SessionExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class LobbyStompController {

    private static final Logger log = LoggerFactory.getLogger(LobbyStompController.class);

    private final GameSessionRepository sessionRepo;
    private final GameSessionService sessions;
    private final LobbyRegistry registry;
    private final SimpMessagingTemplate broker;

    /** sid -> code, so we know which lobby a disconnecting client belonged to. */
    private final ConcurrentHashMap<String, String> sidToCode = new ConcurrentHashMap<>();

    public LobbyStompController(GameSessionRepository sessionRepo,
                                GameSessionService sessions,
                                LobbyRegistry registry,
                                SimpMessagingTemplate broker) {
        this.sessionRepo = sessionRepo;
        this.sessions = sessions;
        this.registry = registry;
        this.broker = broker;
    }

    @MessageMapping("/lobby/{code}/join")
    public void join(@DestinationVariable String code, SimpMessageHeaderAccessor headers) {
        StompPrincipal p = principal(headers);
        if (p == null || !p.isPlayer()) {
            sendUserError(p, "not_a_player");
            return;
        }
        Optional<GameSession> sessOpt = sessionRepo.findByCode(code);
        if (sessOpt.isEmpty()) { sendUserError(p, "invalid_code"); return; }
        GameSession s = sessOpt.get();
        if (!"LOBBY".equals(s.getStatus())) { sendUserError(p, "not_in_lobby"); return; }

        JoinResult result = registry.join(code, s.getMaxPlayers(), headers.getSessionId(), p.getNickname());
        switch (result.status()) {
            case OK -> {
                sidToCode.put(headers.getSessionId(), code);
                sessions.announceJoin(code, result.player());
            }
            case FULL -> sendUserError(p, "lobby_full");
            case NICKNAME_TAKEN -> sendUserError(p, "nickname_taken");
            case NO_LOBBY -> sendUserError(p, "no_lobby");
        }
    }

    @MessageMapping("/lobby/{code}/leave")
    public void leave(@DestinationVariable String code, SimpMessageHeaderAccessor headers) {
        registry.leaveBySession(code, headers.getSessionId())
                .ifPresent(p -> sessions.announceLeave(code, p));
        sidToCode.remove(headers.getSessionId());
    }

    @MessageMapping("/lobby/{code}/start")
    public void start(@DestinationVariable String code, SimpMessageHeaderAccessor headers) {
        StompPrincipal p = principal(headers);
        if (p == null || !p.isAuthenticatedUser()) { sendUserError(p, "not_host"); return; }
        try {
            sessions.startSession(code, p.getUserId());
        } catch (SessionExceptions.HostMismatchException e) {
            sendUserError(p, "not_host");
        } catch (SessionExceptions.EmptyLobbyException e) {
            sendUserError(p, "empty_lobby");
        } catch (SessionExceptions.IllegalSessionStateException e) {
            sendUserError(p, "bad_state");
        } catch (SessionExceptions.SessionNotFoundException e) {
            sendUserError(p, "invalid_code");
        }
    }

    @MessageMapping("/lobby/{code}/cancel")
    public void cancel(@DestinationVariable String code, SimpMessageHeaderAccessor headers) {
        StompPrincipal p = principal(headers);
        if (p == null || !p.isAuthenticatedUser()) { sendUserError(p, "not_host"); return; }
        try {
            sessions.cancelSession(code, p.getUserId());
        } catch (SessionExceptions.HostMismatchException e) {
            sendUserError(p, "not_host");
        } catch (SessionExceptions.SessionNotFoundException e) {
            sendUserError(p, "invalid_code");
        }
    }

    @MessageMapping("/lobby/{code}/kick")
    public void kick(@DestinationVariable String code,
                     Map<String, Object> body,
                     SimpMessageHeaderAccessor headers) {
        StompPrincipal p = principal(headers);
        if (p == null || !p.isAuthenticatedUser()) { sendUserError(p, "not_host"); return; }
        Object nick = body == null ? null : body.get("nickname");
        if (!(nick instanceof String nickname) || nickname.isBlank()) {
            sendUserError(p, "missing_nickname");
            return;
        }
        try {
            sessions.kickPlayer(code, p.getUserId(), nickname);
        } catch (SessionExceptions.HostMismatchException e) {
            sendUserError(p, "not_host");
        } catch (SessionExceptions.IllegalSessionStateException e) {
            sendUserError(p, "bad_state");
        } catch (SessionExceptions.SessionNotFoundException e) {
            sendUserError(p, "invalid_code");
        }
    }

    /** Drop disconnecting players from the in-memory roster. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sid = event.getSessionId();
        String code = sidToCode.remove(sid);
        if (code == null) return;
        registry.leaveBySession(code, sid).ifPresent(p -> {
            try { sessions.announceLeave(code, p); }
            catch (RuntimeException e) { log.warn("announceLeave failed for {}/{}: {}", code, sid, e.toString()); }
        });
    }

    private static StompPrincipal principal(SimpMessageHeaderAccessor headers) {
        return headers.getUser() instanceof StompPrincipal sp ? sp : null;
    }

    private void sendUserError(StompPrincipal p, String reason) {
        if (p == null) return;
        broker.convertAndSendToUser(p.getName(), "/queue/lobby",
                new LobbyEvent("error", Map.of("reason", reason)));
    }
}
