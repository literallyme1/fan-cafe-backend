package com.example.fan_cafe.order.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.user.exception.UserErrorCode;

public enum Status {
    CREATED,
    PAID,
    CANCELLED,
    FAILED;

    public static Status of(String value) {
        try{
            return Status.valueOf(value);
        }catch (IllegalArgumentException e) {
            throw new CustomException(OrderErrorCode.INVALID_STATUS);
        }
    }
}
