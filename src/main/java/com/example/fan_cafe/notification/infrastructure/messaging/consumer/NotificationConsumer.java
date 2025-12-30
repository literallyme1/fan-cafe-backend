package com.example.fan_cafe.notification.infrastructure.messaging.consumer;

import com.example.fan_cafe.notification.application.retry.RetryPolicy;
import com.example.fan_cafe.notification.application.retry.RetryTarget;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.RetryRouter;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.XDeathHeaderReader;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.MAIN_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final RetryPolicy retryPolicy;
    private final XDeathHeaderReader xDeathHeaderReader;
    private final RetryRouter retryRouter;

    @RabbitListener(queues = MAIN_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String eventId = message.getMessageProperties().getMessageId();

        try {
//            process(message);
            channel.basicAck(tag,false);
            //이미 처리된 경우 -> ack
        } catch (DataIntegrityViolationException e) {
            channel.basicAck(tag, false);
        } catch(Exception e) {
            //지금까지 실패 횟수 확인
            int retryCount = xDeathHeaderReader.getRetryCount(message);

            //로직 오류일 시 바로 DLQ
            if (!retryPolicy.isRetryable(e)) {
                log.error(
                        "notification sent to DLQ: eventId={}, retryCount={}",
                        eventId, retryCount
                );
                channel.basicNack(tag, false, false); //마지막 bool , 큐에 넣지 말고 버려라
                return;
            }

            //다음 목적지 결정
            RetryTarget target = retryPolicy.decideRetryTarget(retryCount);

            log.warn(
                    "notification retry: eventId={}, retryCount={}, target={}",
                    eventId, retryCount, target
            );

            //직접 재전송
            retryRouter.routeRetry(message, target, e);

            // 원래 메시지는 정상 처리된 것으로 ack
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); //deliveryTag : 메세지 식별자, 하나만 (false)

        }
    }

}