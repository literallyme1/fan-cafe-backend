package com.example.fan_cafe.outbox.exception;

/**
 * 트랜잭션 안에서 {@code processed_events} INSERT 시 UNIQUE 충돌이 난 경우(동시 소비).
 * 상위에서는 이미 처리된 것과 동일하게 취급하고 Redis 캐시만 맞춘다.
 */
public class DuplicateProcessedEventException extends RuntimeException {

    public DuplicateProcessedEventException(Throwable cause) {
        super("Duplicate processed_events row", cause);
    }
}
