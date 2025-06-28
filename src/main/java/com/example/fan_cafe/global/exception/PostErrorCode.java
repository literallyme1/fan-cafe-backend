package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PostErrorCode implements BaseErrorCode{

    POST_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
;
    private final String code;
    private final HttpStatus status;
    private final String message;

    PostErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

