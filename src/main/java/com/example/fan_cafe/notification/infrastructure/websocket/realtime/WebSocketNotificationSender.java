package com.example.fan_cafe.notification.infrastructure.websocket.realtime;

import com.example.fan_cafe.notification.infrastructure.push.PushSender;
import com.example.fan_cafe.notification.infrastructure.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationSender implements PushSender {
    // 사용자가 온라인일 시 알림 전송 파일
    private final WebSocketSessionRegistry sessionRegistry;

    //온라인 사용자에게 알림 전송
    public void send(Long userId, Object payload) {

        WebSocketSession session = sessionRegistry.getSession(userId);

        try {
            session.sendMessage(
                    new TextMessage(payload.toString())
            );
        } catch (Exception e) {
            //실패 시 log
            log.warn("[WS SEND FAIL] userId={}", userId, e);
        }
    }
}
