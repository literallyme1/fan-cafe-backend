package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum JwtErrorCode implements BaseErrorCode{

    KEY_LOAD_FAILED("U001", HttpStatus.INTERNAL_SERVER_ERROR, "RSA Key"),
    INVALID_REFRESH_TOKEN("U002", HttpStatus.CONFLICT, "리프레시 토큰이 확인되지 않습니다."),
    REFRESH_TOKEN_MISMATCH("U003", HttpStatus.CONFLICT, "리프레시 토큰이 일치하지 않습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    JwtErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

