package com.example.fan_cafe.order.payment.client;

import java.math.BigDecimal;

public record PaymentFailCommand(BigDecimal expectedAmount, String reason) {
}
