package com.example.fan_cafe.notification.infrastructure.messaging.consumer;

import com.example.fan_cafe.notification.application.retry.RetryPolicy;
import com.example.fan_cafe.notification.application.retry.RetryTarget;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.RetryRouter;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.XDeathHeaderReader;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.MAIN_QUEUE;

@RequiredArgsConstructor
@Slf4j
public abstract class NotificationConsumer<T> {

    private final RetryPolicy retryPolicy;
    private final XDeathHeaderReader xDeathHeaderReader;
    private final RetryRouter retryRouter;

    public void consume(T event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            //message -> event
            process(event);
            channel.basicAck(tag, false);
            //이미 처리된 경우 -> ack
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event detected or Data Integrity violation. Acking... eventId: {}", message.getMessageProperties().getMessageId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            handleFailure(message, channel, e);
        }
    }

    private void handleFailure(Message message, Channel channel, Exception e) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        //지금까지 실패 횟수 확인
        int retryCount = xDeathHeaderReader.getRetryCount(message);

        //로직 오류일 시 바로 DLQ
        if (!retryPolicy.isRetryable(e)) {
            log.error(
                    "notification sent to DLQ: retryCount={}",
                    retryCount
            );
            channel.basicNack(tag, false, false); //마지막 bool , 큐에 넣지 말고 버려라
            return;
        }

        //다음 목적지 결정
        RetryTarget target = retryPolicy.decideRetryTarget(retryCount);

        log.warn(
                "notification retry: retryCount={}, target={}",
                retryCount, target
        );

        //직접 재전송
        retryRouter.routeRetry(message, target, e);

        // 원래 메시지는 정상 처리된 것으로 ack
        channel.basicAck(tag, false); //deliveryTag : 메세지 식별자, 하나만 (false)
    }

    protected abstract void process(T event);


}