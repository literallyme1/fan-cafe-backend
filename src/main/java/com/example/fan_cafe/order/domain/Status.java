package com.example.fan_cafe.order.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;

public enum Status {
    PENDING,
    /** Mock PG 승인 대기 — 주문 생성 직후 기본 상태 */
    PAYMENT_PENDING,
    /** Mock PG 승인 완료 — 이 시점에만 Outbox(ORDER_CREATED) 저장 */
    PAID,
    /** 승인 금액 불일치·Mock PG 실패 등으로 결제가 완료되지 않은 상태 */
    PAYMENT_FAILED,
    CANCELLED;

    // 문자열 입력을 enum으로 안전하게 변환하고, 유효하지 않으면 도메인 예외로 통일한다.
    public static Status of(String value) {
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(OrderErrorCode.INVALID_STATUS);
        }
    }
}
