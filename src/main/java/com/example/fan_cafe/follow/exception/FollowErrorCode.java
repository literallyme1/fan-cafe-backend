package com.example.fan_cafe.follow.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FollowErrorCode implements BaseErrorCode {

    SELF_FOLLOW("F001", HttpStatus.BAD_REQUEST, "이미 북마크를 했습니다."),
    ALREADY_FOLLOWED("F002", HttpStatus.BAD_REQUEST, "사용자는 이미 팔로우 하셨습니다."),
    BLOCKED("F003", HttpStatus.BAD_REQUEST, "이미지를 1개 이상 게시해주세요.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    FollowErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
