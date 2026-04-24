package com.example.fan_cafe.order.exception;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OrderErrorCode implements BaseErrorCode {

    // 주문 도메인에서 발생하는 검증/요청 오류 코드를 한 곳에서 관리한다.
    INVALID_STATUS("O001", HttpStatus.BAD_REQUEST, "주문 상태가 올바르지 않습니다."),
    ORDER_ITEMS_REQUIRED("O002", HttpStatus.BAD_REQUEST, "주문 상품은 최소 1개 이상이어야 합니다."),
    INVALID_QUANTITY("O003", HttpStatus.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다."),
    ORDER_NOT_FOUND("O004", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_NOT_CANCELLABLE("O005", HttpStatus.BAD_REQUEST, "현재 상태에서는 주문 취소가 불가능합니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    OrderErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
