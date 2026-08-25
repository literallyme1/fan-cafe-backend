package com.example.fan_cafe.order.saga.interfaces.dto;

import com.example.fan_cafe.order.payment.client.PaymentResultStatus;

public record SagaManualActionResponse(
        SagaAdminResponse saga,
        PaymentResultStatus observedPaymentStatus
) {
}
