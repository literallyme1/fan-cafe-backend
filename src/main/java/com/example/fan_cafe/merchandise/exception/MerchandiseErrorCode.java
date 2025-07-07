package com.example.fan_cafe.merchandise.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MerchandiseErrorCode implements BaseErrorCode {

    SCHEDULE_INVALID_TIME("S001", HttpStatus.BAD_REQUEST, "종료일은 시작일보다 앞설 수 없습니다."),
    SCHEDULE_NOT_FOUND("S002", HttpStatus.NOT_FOUND, "스케줄을 찾을 수 없습니다."),
    NO_IMAGE_PROVIDED("S003", HttpStatus.BAD_REQUEST, "이미지를 1개 이상 게시해주세요.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    MerchandiseErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
