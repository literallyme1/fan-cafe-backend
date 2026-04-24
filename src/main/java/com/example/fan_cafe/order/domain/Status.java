package com.example.fan_cafe.order.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;

public enum Status {
    PENDING,
    PAID,
    CANCELLED;

    // 문자열 입력을 enum으로 안전하게 변환하고, 유효하지 않으면 도메인 예외로 통일한다.
    public static Status of(String value) {
        try{
            return Status.valueOf(value);
        }catch (IllegalArgumentException e) {
            throw new CustomException(OrderErrorCode.INVALID_STATUS);
        }
    }
}
