package com.example.fan_cafe.notification.infrastructure.messaging.consumer;

import com.example.fan_cafe.comment.events.messaging.CommentCreatedEvent;
import com.example.fan_cafe.comment.events.messaging.CommentRabbitConstants;
import com.example.fan_cafe.notification.application.NotificationService;

import com.example.fan_cafe.notification.application.retry.RetryPolicy;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.RetryRouter;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.XDeathHeaderReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.amqp.core.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.MAIN_QUEUE;


@Component
@Slf4j
public class CommentNotificationConsumer extends NotificationConsumer<CommentCreatedEvent>{

    private final NotificationService notificationService;

    public CommentNotificationConsumer(
            RetryPolicy retryPolicy,
            XDeathHeaderReader xDeathHeaderReader,
            RetryRouter retryRouter,
            NotificationService notificationService
    ) {
        super(retryPolicy, xDeathHeaderReader, retryRouter);
        this.notificationService = notificationService;
    }


    @RabbitListener(
            queues = MAIN_QUEUE,
            ackMode = "MANUAL"
    )
    //spring 이 인자 채워줌.
    public void consume(CommentCreatedEvent event, Message message, Channel channel) throws IOException {
        super.consume(event, message, channel);
    }

    @Override
    protected void process(CommentCreatedEvent event) {
        //Step 1. 글 주인이 쓴 댓글인 지 확인
        if (event.getPostAuthorId().equals(event.getCommentAuthorId())) return;
        notificationService.createAndDispatchNotification(event);
    }
}
