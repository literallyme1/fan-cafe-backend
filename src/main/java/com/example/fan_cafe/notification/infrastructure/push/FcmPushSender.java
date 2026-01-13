package com.example.fan_cafe.notification.infrastructure.push;

import com.example.fan_cafe.notification.application.PushTokenQueryService;
import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.domain.push.PushToken;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmPushSender implements PushSender{

    private final FcmClient fcmClient;
    private final PushTokenQueryService tokenQueryService;
    //등록된 디바이스 모두에게 알림을 보냄.
    @Override
    public void send(Notification notification) {

        Long receiverId = notification.getReceiverId();

        List<PushToken> tokens = tokenQueryService.findActiveTokens(receiverId);

        if(tokens.isEmpty()) { return; }

        for (PushToken token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .putData("notificationId",
                                notification.getId().toString())
                        .putData("message",
                                notification.getMessage())
                        .build();

                fcmClient.send(message);
                token.markUsed();

            } catch (Exception e) {
                // 실패하면 로그만
                log.warn("[PUSH FAIL] userId={}, token={}",
                        receiverId, token.getToken(), e);
            }
        }
    }

}
