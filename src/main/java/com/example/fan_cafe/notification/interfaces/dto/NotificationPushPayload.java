package com.example.fan_cafe.notification.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationPushPayload(
        @Schema(description = "알림 식별자", example = "4401")
        Long notificationId,
        @Schema(description = "푸시 메시지", example = "주문 결제가 완료되었습니다.")
        String message
) {
}
