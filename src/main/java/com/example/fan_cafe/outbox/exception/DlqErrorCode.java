package com.example.fan_cafe.outbox.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DlqErrorCode implements BaseErrorCode {

    DLQ_EVENT_NOT_FOUND("DQ001", HttpStatus.NOT_FOUND, "DLQ 이벤트를 찾을 수 없습니다."),
    DLQ_NOT_RETRYABLE("DQ002", HttpStatus.BAD_REQUEST, "RETRY_EXCEEDED가 아닌 DLQ는 재발행할 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    DlqErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
