package com.example.fan_cafe.user.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.UserErrorCode;

public enum Role {
    USER, ADMIN;

    public static Role from(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(UserErrorCode.INVALID_ROLE);
        }
    }
}
