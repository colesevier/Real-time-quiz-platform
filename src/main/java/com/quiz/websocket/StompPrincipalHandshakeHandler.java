package com.quiz.websocket;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the HTTP session at WebSocket handshake time and packages identity into a
 * StompPrincipal so STOMP controllers can authorize requests without trusting clients.
 */
public class StompPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        UUID userId = null;
        String username = null;
        String nickname = null;
        String joinedCode = null;

        if (request instanceof ServletServerHttpRequest servletReq) {
            HttpSession session = servletReq.getServletRequest().getSession(false);
            if (session != null) {
                Object u = session.getAttribute("userID");
                if (u instanceof UUID id) userId = id;
                Object n = session.getAttribute("username");
                if (n instanceof String s) username = s;
                Object nick = session.getAttribute("nickname");
                if (nick instanceof String s) nickname = s;
                Object code = session.getAttribute("playerCode");
                if (code instanceof String s) joinedCode = s;
            }
        }

        return new StompPrincipal(userId, username, nickname, joinedCode);
    }
}
