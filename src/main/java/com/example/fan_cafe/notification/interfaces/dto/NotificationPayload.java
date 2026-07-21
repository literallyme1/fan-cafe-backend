package com.example.fan_cafe.notification.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NotificationPayload(
        @Schema(description = "알림 식별자", example = "4401")
        Long notificationId,
        @Schema(description = "알림 메시지", example = "주문 ORD-20260721-001의 결제가 완료되었습니다.")
        String message,
        @Schema(description = "알림 생성 시각", example = "2026-07-21T18:31:00")
        LocalDateTime createdAt
) {
}
