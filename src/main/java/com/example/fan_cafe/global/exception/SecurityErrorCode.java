package com.example.fan_cafe.global.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SecurityErrorCode implements BaseErrorCode{

    UNAUTHORIZED("U001",HttpStatus.UNAUTHORIZED, "로그인되지 않은 사용자입니다."),
    FORBIDDEN("U002", HttpStatus.FORBIDDEN, "이 기능을 사용할 권한이 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    SecurityErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
