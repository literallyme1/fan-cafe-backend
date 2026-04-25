package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.notification.application.NotificationDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxConsumer {

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = OUTBOX_QUEUE, ackMode = "MANUAL")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            Long receiverId = extractReceiverId(payload);
            dispatcher.dispatch(receiverId, payload);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.warn("[OUTBOX CONSUME FAIL] payload={}", payload, e);
            channel.basicNack(tag, false, false);
        }
    }

    private Long extractReceiverId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.hasNonNull("receiverId")) {
                return node.get("receiverId").asLong();
            }
            if (node.hasNonNull("userId")) {
                return node.get("userId").asLong();
            }
        } catch (Exception ignored) {
            // 파싱 실패는 아래 예외로 처리된다.
        }
        throw new IllegalArgumentException("Receiver id not found in payload");
    }
}
