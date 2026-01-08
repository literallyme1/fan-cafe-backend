package com.example.fan_cafe.notification.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    //WebSocket 접속 종료 시 명단 갱신 리스너

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");

        if(userId == null) {
            return;
        }

        sessionRegistry.register(userId, session); //온라인 명단 등록
        log.info("[WS CONNECT] userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");

        if (userId == null) { return; }

        sessionRegistry.unregister(userId);
        log.info("[WS DISCONNECT] userId={}, status={}", userId, status);
    }
}
