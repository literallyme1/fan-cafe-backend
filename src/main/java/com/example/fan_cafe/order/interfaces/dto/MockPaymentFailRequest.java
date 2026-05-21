package com.example.fan_cafe.order.interfaces.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MockPaymentFailRequest {

    /** 실패 사유 (선택) — order_status_history.reason에 기록 */
    private String reason;
}
