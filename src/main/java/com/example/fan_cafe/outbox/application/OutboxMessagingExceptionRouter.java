package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.outbox.exception.NonRetryableException;
import com.example.fan_cafe.outbox.exception.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * {@link com.example.fan_cafe.outbox.mq.OutboxConsumer}에서 잡히지 않은 예외를
 * {@link RetryableException} / {@link NonRetryableException} 중 하나로 정규화한다.
 */
@Slf4j
public final class OutboxMessagingExceptionRouter {

    private OutboxMessagingExceptionRouter() {
    }

    /**
     * Retry 큐 vs DLQ 라우팅에 쓸 예외로 변환한다.
     * 이미 분류된 타입·cause 체인·일반적인 일시/영구 오류 규칙을 순서대로 적용한다.
     */
    public static RuntimeException wrapForRouting(Throwable throwable) {
        if (throwable instanceof RetryableException r) {
            return r;
        }
        if (throwable instanceof NonRetryableException n) {
            return n;
        }
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RetryableException r) {
                return r;
            }
            if (current instanceof NonRetryableException n) {
                return n;
            }
            if (current instanceof IllegalArgumentException || current instanceof IllegalStateException) {
                return new NonRetryableException(current.getMessage(), current);
            }
            if (current instanceof CustomException c) {
                return new NonRetryableException(c.getMessage(), c);
            }
            if (current instanceof IOException
                    || current instanceof SocketTimeoutException
                    || current instanceof TransientDataAccessException) {
                return new RetryableException("transient failure", current);
            }
            current = current.getCause();
        }
        // 무한 재시도 방지: 알 수 없는 오류는 DLQ로 보내 수동 조사 대상으로 둔다.
        log.warn("[OUTBOX ROUTING] unclassified exception, routing to DLQ: {}", throwable.toString());
        return new NonRetryableException("unclassified consumer failure", throwable);
    }
}
