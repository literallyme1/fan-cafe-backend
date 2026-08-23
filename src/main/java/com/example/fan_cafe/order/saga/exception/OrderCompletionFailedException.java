package com.example.fan_cafe.order.saga.exception;

/** Payment 승인 이후 Order 완료 로컬 트랜잭션이 실패했음을 나타내는 내부 Saga 신호. */
public class OrderCompletionFailedException extends RuntimeException {

    public OrderCompletionFailedException(Throwable cause) {
        super("Order completion failed after payment approval", cause);
    }
}
