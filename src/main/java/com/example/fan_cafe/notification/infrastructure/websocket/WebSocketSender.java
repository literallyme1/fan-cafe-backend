package com.example.fan_cafe.notification.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketSender {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Long userId, Object payload) {
        messagingTemplate.convertAndSend(
                "/queue/notifications" + userId,
                payload
        );
    }
}
