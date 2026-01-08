package com.example.fan_cafe.notification.interfaces.dto;

import java.time.LocalDateTime;

public record NotificationPayload(
        Long notificationId,
        String message,
        LocalDateTime createdAt
) {
}
