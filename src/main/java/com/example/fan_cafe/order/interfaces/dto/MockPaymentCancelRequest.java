package com.example.fan_cafe.order.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class MockPaymentCancelRequest {
    //취소 이유
    @NotBlank(message = "취소 사유는 필수입니다.") 
    @Schema(description = "환불 사유", example = "고객 요청에 따른 전체 환불")
    private String cancelReason;

    @NotNull(message = "sagaId는 필수입니다.")
    @Schema(description = "Step 2 환불 요청 식별 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID sagaId;
}
