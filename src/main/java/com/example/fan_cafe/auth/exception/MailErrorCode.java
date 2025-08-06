package com.example.fan_cafe.auth.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;

@Getter
public enum MailErrorCode implements BaseErrorCode {

    MAIL_SEND_FAILED("M001", HttpStatus.INTERNAL_SERVER_ERROR, "메일 전송에 실패했습니다."),
    EMAIL_NOT_FOUND("M002", HttpStatus.BAD_REQUEST, "해당 이메일로 가입된 사용자가 없습니다"),
    INVALID_TOKEN("M003", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_TOKEN_PURPOSE("M004", HttpStatus.BAD_REQUEST, "잘못된 토큰 목적입니다."),
    PASSWORD_ALREADY_CHANGED("M005", HttpStatus.BAD_REQUEST, "이미 비밀번호가 변경된 토큰입니다."),
    PASSWORD_SAME_AS_OLD("M006",HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일합니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    MailErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

