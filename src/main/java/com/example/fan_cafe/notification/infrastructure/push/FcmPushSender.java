package com.example.fan_cafe.notification.infrastructure.push;

import com.example.fan_cafe.notification.application.PushTokenQueryService;
import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.domain.push.PushToken;
import com.google.firebase.messaging.FirebaseMessagingException;
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

        log.info("[PUSH TOKEN COUNT] userId={}, count={}",
                receiverId, tokens.size());
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
                // 전송할 시 fcm 이 주는 정보 update
                if (isInvalidToken(e)) {
                    log.info("[PUSH SKIP] no push token userId={}", receiverId);
                    // 토큰 무효 → 비활성화
                    token.deactivate();
                }
                // 실패하면 로그만
                log.warn("[PUSH FAIL] userId={}, token={}",
                        receiverId, token.getToken(), e);
            }
        }
    }

    private boolean isInvalidToken(Exception e) {

        //e 가 FireException 이면 fme 의 true, false
        return e instanceof FirebaseMessagingException fme
                && fme.getMessagingErrorCode() != null;
    }

}
