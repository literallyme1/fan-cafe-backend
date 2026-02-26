package com.example.fan_cafe.order.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OrderErrorCode implements BaseErrorCode {

    INVALID_STATUS("O001", HttpStatus.BAD_REQUEST, "주문 상태가 올바르지 않습니다."),
    MERCHANDISE_NOT_FOUND("M002", HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    INVALID_MERCHANDISE_PROPERTY("M003", HttpStatus.INTERNAL_SERVER_ERROR, "상품의 속성을 찾을 수 없습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    OrderErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
