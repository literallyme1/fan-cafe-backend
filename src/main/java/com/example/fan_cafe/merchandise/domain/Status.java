package com.example.fan_cafe.merchandise.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.exception.UserErrorCode;

public enum Status {
    SALE, //판매
    SOLD_OUT,
    HIDDEN,
    STOP_SELLING,
    DELETED;

    public static Status from(String value) {
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(UserErrorCode.INVALID_ROLE);
        }
    }
}
