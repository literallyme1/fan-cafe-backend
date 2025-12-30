package com.example.fan_cafe.notification.infrastructure.messaging.consumer.config;

import com.example.fan_cafe.comment.events.messaging.CommentCreatedEvent;
import com.example.fan_cafe.comment.events.messaging.CommentRabbitConstants;
import com.example.fan_cafe.notification.application.NotificationService;

import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(
            queues = CommentRabbitConstants.COMMENT_QUEUE,
            ackMode = "MANUAL" //ack 를 수동 제어
    )
    public void consume(
            CommentCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            //Step 1. 글 주인이 쓴 댓글인 지 확인
            if (event.getPostAuthorId()
                    .equals(event.getCommentAuthorId())) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            //Step 2. 알림 저장
            notificationService.saveNotification(
                    event.getPostAuthorId(),
                    event.getEventId(),
                    "내 게시글에 댓글이 달렸습니다."
            );

            //Step 3. 정상 처리 ACK
            channel.basicAck(deliveryTag, false);

        } //event 가 존재하는 경우
        catch (DataIntegrityViolationException e) {
            log.info("Duplicate eventId: {}", event.getEventId());
            channel.basicAck(deliveryTag, false); //queue 에서 지우기
        } catch (Exception e){
            // Step 5. 처리 실패 → 재시도
            log.error("Notification consume failed", e);
            channel.basicNack(deliveryTag, false, false); //다시 큐에 넣어서 재시도
        }
    }
}
