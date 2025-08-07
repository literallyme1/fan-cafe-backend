package com.example.fan_cafe.bookmark.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BookmarkErrorCode implements BaseErrorCode {

    ALREADY_MARKED("B001", HttpStatus.BAD_REQUEST, "이미 북마크를 했습니다."),
    MARKED_NOT_FOUND("B002", HttpStatus.NOT_FOUND, "북마크 기록을 찾을 수 없습니다."),
    NO_IMAGE_PROVIDED("B003", HttpStatus.BAD_REQUEST, "이미지를 1개 이상 게시해주세요.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    BookmarkErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
