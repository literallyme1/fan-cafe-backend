package com.example.fan_cafe.notification.interfaces.dto;

import com.example.fan_cafe.notification.domain.push.PushPlatform;

public record PushTokenRequest(
        String token,
        PushPlatform platform
) {
}
