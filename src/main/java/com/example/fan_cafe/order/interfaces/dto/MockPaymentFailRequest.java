package com.example.fan_cafe.order.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MockPaymentFailRequest {

    /** 실패 사유 (선택) — order_status_history.reason에 기록 */
    @Schema(description = "결제 실패 사유", example = "카드 승인 거절")
    private String reason;
}
