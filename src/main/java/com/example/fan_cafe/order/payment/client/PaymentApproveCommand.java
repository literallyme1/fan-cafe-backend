package com.example.fan_cafe.order.payment.client;

import java.math.BigDecimal;

public record PaymentApproveCommand(BigDecimal expectedAmount, BigDecimal approvalAmount, String paymentKey) {
}
