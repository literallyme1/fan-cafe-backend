package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements BaseErrorCode{

    USER_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD("U002", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    USER_ALREADY_EXISTS("U003", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    UserErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

