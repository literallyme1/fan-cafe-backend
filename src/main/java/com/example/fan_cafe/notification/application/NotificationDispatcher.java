package com.example.fan_cafe.notification.application;

import com.example.fan_cafe.notification.infrastructure.push.FcmPushSender;
import com.example.fan_cafe.notification.infrastructure.websocket.realtime.WebSocketNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final PushDecisionService pushDecisionService;
    private final WebSocketNotificationSender websocketSender;
    private final FcmPushSender pushSender;

    public void dispatch(Long receiverId, Object payload) {
        if (pushDecisionService.isOnline(receiverId)) {
            websocketSender.send(receiverId, payload);
            return;
        }

        log.info("[PUSH DECISION] offline userId={}", receiverId);
        pushSender.send(receiverId, payload);
    }
}
