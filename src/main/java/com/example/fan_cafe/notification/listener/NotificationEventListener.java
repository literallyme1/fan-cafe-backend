package com.example.fan_cafe.notification.listener;


import com.example.fan_cafe.notification.adapter.SlackWebhookClient;
import com.example.fan_cafe.notification.domain.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SlackWebhookClient slackWebhookClient;

    //이벤트 수신 함수
    @Async("notificationExceutor")
    @EventListener
    public void handle(NotificationEvent event) {

        slackWebhookClient.send(event);
    }
}
