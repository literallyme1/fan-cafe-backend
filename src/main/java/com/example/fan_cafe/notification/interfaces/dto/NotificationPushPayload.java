package com.example.fan_cafe.notification.interfaces.dto;

public record NotificationPushPayload(
        Long notificationId,
        String message
) {
}
