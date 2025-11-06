package com.example.fan_cafe.follow.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FollowErrorCode implements BaseErrorCode {

    SELF_FOLLOW("F001", HttpStatus.BAD_REQUEST, "이미 북마크를 했습니다."),
    ALREADY_FOLLOWED("F002", HttpStatus.BAD_REQUEST, "사용자는 이미 팔로우 하셨습니다."),
    FOLLOW_NOT_FOUND("F003", HttpStatus.NOT_FOUND, "Follow 여부가 없습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    FollowErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
