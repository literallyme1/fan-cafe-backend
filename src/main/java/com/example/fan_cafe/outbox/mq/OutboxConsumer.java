package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.outbox.application.OutboxMessageProcessingService;
import com.example.fan_cafe.outbox.application.OutboxMessagingExceptionRouter;
import com.example.fan_cafe.outbox.exception.NonRetryableException;
import com.example.fan_cafe.outbox.exception.RetryableException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_QUEUE;

/**
 * Outbox 메인 큐 소비. 처리 성공 시에만 ACK하고,
 * 일시 오류는 Retry 큐·구조적 오류는 DLQ로 넘긴 뒤 원 메시지는 ACK하여 브로커 재전달 루프를 끊는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxConsumer {

    private final OutboxMessageProcessingService messageProcessingService;
    private final OutboxFailureRoutingPublisher outboxFailureRoutingPublisher;

    @RabbitListener(queues = OUTBOX_QUEUE, ackMode = "MANUAL")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            // DB/트랜잭션까지 성공해야 여기까지 도달 → 그때 ACK
            messageProcessingService.process(payload);
            channel.basicAck(tag, false);
        } catch (RetryableException e) {
            log.warn("[OUTBOX RETRY ROUTE] {}", e.getMessage(), e);
            outboxFailureRoutingPublisher.publishToRetryQueue(payload);
            channel.basicAck(tag, false);
        } catch (NonRetryableException e) {
            log.error("[OUTBOX DLQ ROUTE] {}", e.getMessage(), e);
            outboxFailureRoutingPublisher.publishToDlq(payload);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // 명시적 Retryable/NonRetryable가 아닌 경우(예: IllegalArgumentException) 분류
            RuntimeException routed = OutboxMessagingExceptionRouter.wrapForRouting(e);
            if (routed instanceof RetryableException re) {
                log.warn("[OUTBOX RETRY ROUTE] (classified) {}", re.getMessage(), re);
                outboxFailureRoutingPublisher.publishToRetryQueue(payload);
            } else {
                NonRetryableException ne = (NonRetryableException) routed;
                log.error("[OUTBOX DLQ ROUTE] (classified) {}", ne.getMessage(), ne);
                outboxFailureRoutingPublisher.publishToDlq(payload);
            }
            channel.basicAck(tag, false);
        }
    }
}
