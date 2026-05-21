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
    ORDER_NOT_CANCELLABLE("O005", HttpStatus.BAD_REQUEST, "현재 상태에서는 주문 취소가 불가능합니다."),
    PAYMENT_KEY_REQUIRED("O006", HttpStatus.BAD_REQUEST, "idempotencyKey 또는 mockPaymentKey가 필요합니다."),
    INVALID_PAYMENT_STATE("O007", HttpStatus.BAD_REQUEST, "현재 주문 상태에서는 결제 처리를 할 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH("O008", HttpStatus.BAD_REQUEST, "승인 금액이 주문 금액과 일치하지 않습니다."),
    ORDER_ALREADY_PAID("O009", HttpStatus.CONFLICT, "이미 결제 완료된 주문입니다."),
    WEBHOOK_SIGNATURE_INVALID("O010", HttpStatus.UNAUTHORIZED, "웹훅 서명이 유효하지 않습니다."),
    WEBHOOK_TIMESTAMP_INVALID("O011", HttpStatus.BAD_REQUEST, "웹훅 타임스탬프 형식이 올바르지 않습니다."),
    WEBHOOK_TIMESTAMP_EXPIRED("O012", HttpStatus.BAD_REQUEST, "웹훅 타임스탬프가 만료되었습니다."),
    INVALID_WEBHOOK_EVENT_TYPE("O013", HttpStatus.BAD_REQUEST, "지원하지 않는 웹훅 eventType입니다."),
    WEBHOOK_APPROVAL_AMOUNT_REQUIRED("O014", HttpStatus.BAD_REQUEST, "PAYMENT_APPROVED 웹훅에는 approvalAmount가 필요합니다."),
    CANCEL_IDEMPOTENCY_KEY_REQUIRED("O015", HttpStatus.BAD_REQUEST, "취소 idempotencyKey가 필요합니다."),
    ORDER_NOT_REFUNDABLE("O016", HttpStatus.BAD_REQUEST, "결제 완료(PAID) 상태에서만 Mock 취소/환불이 가능합니다."),
    ORDER_ALREADY_REFUNDED("O017", HttpStatus.CONFLICT, "이미 취소/환불 처리된 주문입니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    OrderErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
