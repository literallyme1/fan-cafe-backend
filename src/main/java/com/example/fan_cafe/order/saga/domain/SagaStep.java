package com.example.fan_cafe.order.saga.domain;

public enum SagaStep {
    PAYMENT_APPROVAL,
    PAYMENT_STATUS_CHECK,
    ORDER_COMPLETION,
    PAYMENT_REFUND,
    DONE
}
