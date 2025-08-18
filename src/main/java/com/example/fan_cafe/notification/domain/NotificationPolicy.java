package com.example.fan_cafe.notification.domain;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class NotificationPolicy {
    public boolean canNotify(Long receiverId, Long actorId) {
        // 자기 자신이면 알림 안 보냄
        return !Objects.equals(receiverId, actorId);
    }
}
