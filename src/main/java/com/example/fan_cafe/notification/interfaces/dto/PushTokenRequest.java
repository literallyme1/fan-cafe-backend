package com.example.fan_cafe.notification.interfaces.dto;

import com.example.fan_cafe.notification.domain.push.PushPlatform;
import io.swagger.v3.oas.annotations.media.Schema;

public record PushTokenRequest(
        @Schema(description = "FCM 기기 토큰", example = "fcm_7X9abCDefGhIjKlmNop")
        String token,
        @Schema(description = "기기 플랫폼", example = "ANDROID")
        PushPlatform platform
) {
}
