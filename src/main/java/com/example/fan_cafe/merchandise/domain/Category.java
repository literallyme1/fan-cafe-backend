package com.example.fan_cafe.merchandise.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.exception.UserErrorCode;

public enum Category {

    POSTER,
    T_SHIRT,
    LIGHT_STICK;

    public static Category from(String value) {
        try {
            return Category.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(UserErrorCode.INVALID_ROLE);
        }
    }
}
