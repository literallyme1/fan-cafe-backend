package com.example.fan_cafe.outbox.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.outbox.mq.OutboxMqRetryHeaders.X_RETRY_COUNT;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_DLQ_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_1M_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_30S_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_5S_ROUTING_KEY;

/**
 * 일시 오류 시 단계별 retry 큐로, 영구 오류·재시도 초과 시 DLQ로 원본 페이로드를 보낸다.
 *
 * <p>헤더 {@link OutboxMqRetryHeaders#X_RETRY_COUNT}로 차수를 들고 다니며,
 * 숫자만으로 재시도 단계를 제한해 브로커·애플리케이션 모두에서 무한 재전달 루프에 빠지지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class OutboxFailureRoutingPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxDlqSlackNotifier outboxDlqSlackNotifier;

    /**
     * 컨슈머가 계산한 다음 재시도 차수를 헤더에 실어, 지연 큐(5s/30s/1m 중 하나)로 보낸다.
     *
     * @param headerRetryCount 재발행 후 메인 큐에서 읽힐 {@code x-retry-count} 값(항상 1 이상)
     */
    public void publishToRetryQueue(String payload, int headerRetryCount) {
        int tierAfterFailure = headerRetryCount - 1;
        String routingKey = switch (tierAfterFailure) {
            case 0 -> OUTBOX_RETRY_5S_ROUTING_KEY;
            case 1 -> OUTBOX_RETRY_30S_ROUTING_KEY;
            case 2 -> OUTBOX_RETRY_1M_ROUTING_KEY;
            default -> throw new IllegalArgumentException(
                    "retry routing tier out of range for headerRetryCount=" + headerRetryCount
            );
        };
        rabbitTemplate.convertAndSend(OUTBOX_EXCHANGE, routingKey, payload, message -> {
            message.getMessageProperties().setHeader(X_RETRY_COUNT, headerRetryCount);
            return message;
        });
    }

    /**
     * 자동 복구 불가·재시도 상한 초과 메시지를 DLQ에 적재하고 Slack으로 알린다.
     */
    public void publishToDlq(String payload, int retryCount, String errorMessage, DlqRoutingType routingType) {
        rabbitTemplate.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_DLQ_ROUTING_KEY, payload, message -> {
            message.getMessageProperties().setHeader(X_RETRY_COUNT, retryCount);
            return message;
        });
        outboxDlqSlackNotifier.notifyDlq(payload, retryCount, errorMessage, routingType);
    }
}
