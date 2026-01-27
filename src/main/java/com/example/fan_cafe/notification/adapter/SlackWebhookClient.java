package com.example.fan_cafe.notification.adapter;



import com.example.fan_cafe.global.config.SlackProperties;
import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.notification.formatter.SlackMessageFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

//slack 에 보내기 기능을 담당하는 webhook 전용 client
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackWebhookClient {

    private final SlackProperties slackProperties;
    private final WebClient webClient = WebClient.create(); //http client

    //slack 알림 전송
    public void send(NotificationEvent event) {

        //1. slack 비활성 환경에서는 아무것도 x
        if(!slackProperties.isEnabled()) {
            return;
        }

        try{
            webClient.post()
                    .uri(slackProperties.getUrl())
                    .bodyValue(SlackMessageFormatter.format(event)) //json payload
                    .retrieve() //요청 실행
                    .bodyToMono(Void.class) //응답 바디 필요 x
                    .block();

        }catch (Exception e){ //예외는 로그만
            log.error("[Slack Notification Failed] title={}", event.getTitle(), e);
        }
    }
}
