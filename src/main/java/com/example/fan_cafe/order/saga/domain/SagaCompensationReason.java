package com.example.fan_cafe.order.saga.domain;

public enum SagaCompensationReason {
    ORDER_FAILURE,
    USER_REFUND,
    CAMPAIGN_FAILED,
    LATE_APPROVAL_AFTER_DEADLINE
}
