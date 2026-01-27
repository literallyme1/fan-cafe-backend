package com.example.fan_cafe.notification.guard;


import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//같은 종류 알림 Slack 에 계속 알림이 가는 걸 막음
@Component
public class NotificationRateLimiter {

    // 재알림 허용 시간(1분)
    private static final long COOL_TIME_MS = 60_000;

    //보낸 시간 저장, ConcurrentHashMap 멀티스레드 환경에서도 안전함.
    private final Map<String, Long> lastSentAt = new ConcurrentHashMap<>();

    //알림 보내도 되는 지 확인
    public boolean allow(String key) {
        long now = Instant.now().toEpochMilli();
        Long last = lastSentAt.get(key);

        if (last == null || now - last > COOL_TIME_MS) {
            lastSentAt.put(key, now);
            return true;
        }
        return false;
    }
}
