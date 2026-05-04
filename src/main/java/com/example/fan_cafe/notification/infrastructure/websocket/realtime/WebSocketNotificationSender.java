package com.example.fan_cafe.notification.infrastructure.websocket.realtime;

import com.example.fan_cafe.notification.infrastructure.push.MessageSender;
import com.example.fan_cafe.notification.infrastructure.websocket.WebSocketSessionRegistry;
import com.example.fan_cafe.outbox.exception.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationSender implements MessageSender {
    // 사용자가 온라인일 시 알림 전송 파일
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * 온라인 사용자에게 WebSocket으로 전송.
     * 세션 불가·전송 IOException 등은 Outbox 경로에서 재시도할 수 있도록 {@link RetryableException}으로 올린다.
     */
    public void send(Long userId, Object payload) {

        WebSocketSession session = sessionRegistry.getSession(userId);
        if (session == null || !session.isOpen()) {
            // 온라인 판정과 세션 사이 레이스 등 — 일시적으로 보고 Retry 큐
            throw new RetryableException("WebSocket session unavailable userId=" + userId);
        }

        try {
            session.sendMessage(new TextMessage(payload.toString()));
        } catch (Exception e) {
            log.warn("[WS SEND FAIL] userId={}", userId, e);
            throw new RetryableException("WebSocket send failed userId=" + userId, e);
        }
    }
}
