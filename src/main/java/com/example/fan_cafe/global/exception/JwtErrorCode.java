package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum JwtErrorCode implements BaseErrorCode{

    KEY_LOAD_FAILED("U001", HttpStatus.INTERNAL_SERVER_ERROR, "RSA Key"),
    PRIVATE_KEY_CREATED_FAILED("U002", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    PUBLIC_KEY_CREATED_FAILED("U003", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    JwtErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

