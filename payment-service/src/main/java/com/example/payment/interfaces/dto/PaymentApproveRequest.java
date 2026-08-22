package com.example.payment.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentApproveRequest(
        @NotNull BigDecimal expectedAmount,
        @NotNull BigDecimal approvalAmount,
        String paymentKey
) {
}
