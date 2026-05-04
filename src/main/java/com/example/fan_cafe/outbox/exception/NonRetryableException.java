package com.example.fan_cafe.outbox.exception;

/**
 * 구조적 문제(잘못된 페이로드, 비즈니스 규칙 위반 등 재시도해도 의미 없는 오류).
 * {@link com.example.fan_cafe.outbox.mq.OutboxConsumer}에서 DLQ로 라우팅한다.
 */
public class NonRetryableException extends RuntimeException {

    public NonRetryableException(String message) {
        super(message);
    }

    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
