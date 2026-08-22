package com.example.fan_cafe.order.payment.client;

import java.util.UUID;

public record PaymentRefundCommand(UUID sagaId, String reason) {
}
