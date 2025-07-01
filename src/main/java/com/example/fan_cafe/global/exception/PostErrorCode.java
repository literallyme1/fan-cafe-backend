package com.example.fan_cafe.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PostErrorCode implements BaseErrorCode{

    POST_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_NOT_OWNER("U001", HttpStatus.FORBIDDEN, "게시글의 주인이 아닙니다."),
    NO_IMAGE_PROVIDED("U002", HttpStatus.BAD_REQUEST, "이미지를 1개 이상 게시해주세요.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    PostErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
