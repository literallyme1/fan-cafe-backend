package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements BaseErrorCode{

    USER_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_ALREADEY_DELETED("U002", HttpStatus.BAD_REQUEST, "이미 탈퇴한 사용자입니다."),
    INVALID_PASSWORD("U003", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    EMAIL_ALREADY_EXISTS("U004", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    NICKNAME_ALREADY_EXISTS("U005", HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    INVALID_ROLE("U006", HttpStatus.BAD_REQUEST, "역할이 맞지 않습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    UserErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

