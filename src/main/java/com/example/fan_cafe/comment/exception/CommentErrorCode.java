package com.example.fan_cafe.comment.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentErrorCode implements BaseErrorCode {

    POST_NOT_FOUND("C001", HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND("C002", HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    INVALID_COMMENT_DEPTH("C003", HttpStatus.BAD_REQUEST, "대댓글은 1단계까지만 허용됩니다."),
    COMMENT_NOT_OWNER("C004", HttpStatus.FORBIDDEN, "댓글의 주인이 아닙니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    CommentErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
