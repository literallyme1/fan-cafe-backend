package com.example.payment.interfaces.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentRefundRequest(
        @NotNull UUID sagaId,
        String reason
) {
}
