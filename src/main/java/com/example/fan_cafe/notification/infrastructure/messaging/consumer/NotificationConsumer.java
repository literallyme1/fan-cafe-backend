package com.example.fan_cafe.notification.infrastructure.messaging.consumer;

import com.example.fan_cafe.notification.application.retry.RetryPolicy;
import com.example.fan_cafe.notification.application.retry.RetryTarget;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.RetryRouter;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.XDeathHeaderReader;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.MAIN_QUEUE;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final RetryPolicy retryPolicy;
    private final XDeathHeaderReader xDeathHeaderReader;
    private final RetryRouter retryRouter;

    @RabbitListener(queues = MAIN_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        try {
//            process(message);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch(Exception e) {
            //로직 오류일 시 바로 DLQ
            if (!retryPolicy.isRetryable(e)) {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false); //마지막 bool , 큐에 넣지 말고 버려라
                return;
            }

            //지금까지 실패 횟수 확인
            int retryCount = xDeathHeaderReader.getRetryCount(message);

            //다음 목적지 결정
            RetryTarget target = retryPolicy.decideRetryTarget(retryCount);

            //직접 재전송
            retryRouter.routeRetry(message, target);

            // 원래 메시지는 정상 처리된 것으로 ack
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); //deliveryTag : 메세지 식별자, 하나만 (false)

        }
    }

}