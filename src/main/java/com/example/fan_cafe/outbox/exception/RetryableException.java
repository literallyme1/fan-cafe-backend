package com.example.fan_cafe.outbox.exception;

/**
 * 일시적 장애(네트워크, WebSocket 전송 실패, FCM 일시 오류 등).
 * {@link com.example.fan_cafe.outbox.mq.OutboxConsumer}에서 Retry 큐로 라우팅한다.
 */
public class RetryableException extends RuntimeException {

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
