package com.quiz.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ServerEndpoint(value = "/ws/game/{code}")
public class GameSocket {
    // Very small in-memory map for lobby participants per code.
    private static final Map<String, Set<Session>> rooms = Collections.synchronizedMap(new HashMap<>());

    @OnOpen
    public void onOpen(Session session, @PathParam("code") String code) {
        rooms.computeIfAbsent(code, k -> Collections.synchronizedSet(new HashSet<>())).add(session);
    }

    @OnMessage
    public void onMessage(Session session, @PathParam("code") String code, String message) throws IOException {
        // Broadcast incoming messages to all participants in the room.
        var set = rooms.get(code);
        if (set != null) {
            synchronized (set) {
                for (Session s : set) {
                    if (s.isOpen()) s.getBasicRemote().sendText(message);
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("code") String code) {
        var set = rooms.get(code);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) rooms.remove(code);
        }
    }
}
