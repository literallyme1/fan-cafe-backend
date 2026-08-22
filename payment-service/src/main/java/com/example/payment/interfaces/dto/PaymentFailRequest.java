package com.example.payment.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentFailRequest(@NotNull BigDecimal expectedAmount, String reason) {
}
