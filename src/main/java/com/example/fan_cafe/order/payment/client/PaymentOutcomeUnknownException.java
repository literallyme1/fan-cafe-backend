package com.example.fan_cafe.order.payment.client;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;

public class PaymentOutcomeUnknownException extends CustomException {
    public PaymentOutcomeUnknownException(OrderErrorCode errorCode) {
        super(errorCode);
    }
}
