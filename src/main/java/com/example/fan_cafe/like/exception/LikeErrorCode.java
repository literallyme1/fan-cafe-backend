package com.example.fan_cafe.like.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LikeErrorCode implements BaseErrorCode {

    ALREADY_LIKED("L001", HttpStatus.BAD_REQUEST, "이미 좋아요를 눌렀습니다."),
    LIKED_NOT_FOUND("L002", HttpStatus.NOT_FOUND, "좋아요 기록을 찾을 수 없습니다."),
    SELF_LIKE_FORBIDDEN("L003", HttpStatus.FORBIDDEN, "자신의 글에는 좋아요를 누를 수 없습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    LikeErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
