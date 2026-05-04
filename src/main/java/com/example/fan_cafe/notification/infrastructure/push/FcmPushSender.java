package com.example.fan_cafe.notification.infrastructure.push;

import com.example.fan_cafe.outbox.exception.RetryableException;
import com.example.fan_cafe.notification.application.PushTokenQueryService;
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
public class FcmPushSender implements MessageSender {

    private final FcmClient fcmClient;
    private final PushTokenQueryService tokenQueryService;
    //등록된 디바이스 모두에게 알림을 보냄.
    @Override
    public void send(Long userId, Object payload) {

        List<PushToken> tokens = tokenQueryService.findActiveTokens(userId);

        log.info("[PUSH TOKEN COUNT] userId={}, count={}",
                userId, tokens.size());
        if (tokens.isEmpty()) {
            return;
        }

        boolean anySuccess = false;
        // 무효 토큰이 아닌 예외(네트워크·FCM 일시 장애 등)가 하나라도 있었는지
        boolean transientFailure = false;

        for (PushToken token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .putData("message",
                                payload.toString())
                        .build();

                fcmClient.send(message);
                token.markUsed();
                anySuccess = true;

            } catch (Exception e) {
                if (isInvalidToken(e)) {
                    log.info("[PUSH SKIP] invalid token userId={}", userId);
                    token.deactivate();
                } else {
                    transientFailure = true;
                }
                log.warn("[PUSH FAIL] userId={}, token={}",
                        userId, token.getToken(), e);
            }
        }

        // 전부 실패했고 그중 일시 장애만 있다면 Outbox가 Retry 큐로 넘길 수 있게 한다.
        if (!anySuccess && transientFailure) {
            throw new RetryableException("FCM delivery failed (transient) userId=" + userId);
        }
    }

    private boolean isInvalidToken(Exception e) {

        //e 가 FireException 이면 fme 의 true, false
        return e instanceof FirebaseMessagingException fme
                && fme.getMessagingErrorCode() != null;
    }

}
