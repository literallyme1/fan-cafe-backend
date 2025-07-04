package com.example.fan_cafe.schedule.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ScheduleErrorCode implements BaseErrorCode {

    SCHEDULE_INVALID_TIME("S001", HttpStatus.BAD_REQUEST, "종료일은 시작일보다 앞설 수 없습니다."),
    POST_NOT_OWNER("S002", HttpStatus.FORBIDDEN, "게시글의 주인이 아닙니다."),
    NO_IMAGE_PROVIDED("S003", HttpStatus.BAD_REQUEST, "이미지를 1개 이상 게시해주세요.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    ScheduleErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
