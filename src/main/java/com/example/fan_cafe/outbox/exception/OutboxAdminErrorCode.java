package com.example.fan_cafe.outbox.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OutboxAdminErrorCode implements BaseErrorCode {

    OUTBOX_EVENT_NOT_FOUND("OB001", HttpStatus.NOT_FOUND, "Outbox 이벤트를 찾을 수 없습니다."),
    OUTBOX_EVENT_NOT_MANUAL_REQUIRED("OB002", HttpStatus.BAD_REQUEST, "MANUAL_REQUIRED 상태의 이벤트만 수동 재시도할 수 있습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    OutboxAdminErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
