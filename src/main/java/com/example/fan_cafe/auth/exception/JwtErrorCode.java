package com.example.fan_cafe.auth.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum JwtErrorCode implements BaseErrorCode {

    KEY_LOAD_FAILED("U001", HttpStatus.INTERNAL_SERVER_ERROR, "RSA Key"),
    INVALID_REFRESH_TOKEN("U002", HttpStatus.CONFLICT, "리프레시 토큰이 확인되지 않습니다."),
    REFRESH_TOKEN_MISMATCH("U003", HttpStatus.CONFLICT, "리프레시 토큰이 일치하지 않습니다."),

    UNAUTHORIZED("G020", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("G021", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    TOKEN_EXPIRED("G022", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN("G023", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_REQUIRED("G024", HttpStatus.UNAUTHORIZED, "토큰이 필요합니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    JwtErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

