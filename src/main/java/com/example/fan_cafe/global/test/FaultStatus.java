package com.example.fan_cafe.global.test;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 프로파일에서만 등록되는 인메모리 장애 스위치.
 * 운영 빈에는 존재하지 않도록 {@code test} 프로파일로 한정한다.
 */
@Component
@Profile({"test", "ci", "awstest"})
public class FaultStatus {

    private volatile boolean notificationBlocked;

    public boolean isNotificationBlocked() {
        return notificationBlocked;
    }

    public void setNotificationBlocked(boolean notificationBlocked) {
        this.notificationBlocked = notificationBlocked;
    }
}
