package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.outbox.application.OutboxMessageProcessingService;
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

    private final OutboxMessageProcessingService messageProcessingService;

    @RabbitListener(queues = OUTBOX_QUEUE, ackMode = "MANUAL")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            messageProcessingService.process(payload);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.warn("[OUTBOX CONSUME FAIL] payload={}", payload, e);
            channel.basicNack(tag, false, false);
        }
    }
}
