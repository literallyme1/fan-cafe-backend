package com.example.fan_cafe.notification.infrastructure.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {
    //userId를 세션(메모리)에 저장하는 파일

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session){
        sessions.put(userId, session);
    }

    public void unregister(Long userId) {
        sessions.remove(userId);
    }

    public WebSocketSession getSession(Long userId) {
        return sessions.get(userId);
    }

    public boolean isOnline(Long userId) {
        return sessions.containsKey(userId);
    }
}
