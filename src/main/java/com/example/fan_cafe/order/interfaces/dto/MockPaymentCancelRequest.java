package com.example.fan_cafe.order.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MockPaymentCancelRequest {
    //취소 이유
    @NotBlank(message = "취소 사유는 필수입니다.") 
    @Schema(description = "환불 사유", example = "고객 요청에 따른 전체 환불")
    private String cancelReason;

    @NotBlank(message = "idempotencyKey는 필수입니다.")
    @Schema(description = "환불 멱등 키", example = "REFUND-20260721-001")
    private String idempotencyKey;
}
