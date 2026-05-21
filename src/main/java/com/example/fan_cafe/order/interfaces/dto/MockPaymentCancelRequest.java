package com.example.fan_cafe.order.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MockPaymentCancelRequest {

    @NotBlank(message = "취소 사유는 필수입니다.")
    private String cancelReason;

    @NotBlank(message = "idempotencyKey는 필수입니다.")
    private String idempotencyKey;
}
