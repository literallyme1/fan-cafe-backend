package com.example.fan_cafe.order.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;

public enum Status {
    PENDING,
    PAID,
    CANCELLED;

    public static Status of(String value) {
        try{
            return Status.valueOf(value);
        }catch (IllegalArgumentException e) {
            throw new CustomException(OrderErrorCode.INVALID_STATUS);
        }
    }
}
