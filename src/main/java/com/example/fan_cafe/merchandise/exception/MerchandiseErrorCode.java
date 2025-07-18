package com.example.fan_cafe.merchandise.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MerchandiseErrorCode implements BaseErrorCode {

    OUT_OF_STOCK("M001", HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
    MERCHANDISE_NOT_FOUND("M002", HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    INVALID_MERCHANDISE_PROPERTY("M003", HttpStatus.INTERNAL_SERVER_ERROR, "상품의 속성을 찾을 수 없습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    MerchandiseErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
