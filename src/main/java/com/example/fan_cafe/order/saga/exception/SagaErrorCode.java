package com.example.fan_cafe.order.saga.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

public enum SagaErrorCode implements BaseErrorCode {
    SAGA_NOT_FOUND("S001", HttpStatus.NOT_FOUND, "Saga를 찾을 수 없습니다."),
    INVALID_SAGA_TRANSITION("S002", HttpStatus.CONFLICT, "허용되지 않은 Saga 상태 전이입니다."),
    INVALID_MANUAL_ACTION("S003", HttpStatus.CONFLICT, "현재 Saga 상태에서 허용되지 않은 수동 액션입니다."),
    PAYMENT_NOT_APPROVED_FOR_MANUAL_ACTION(
            "S004", HttpStatus.CONFLICT, "확정된 결제 승인 상태가 아니어서 수동 액션을 수행할 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    SagaErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
